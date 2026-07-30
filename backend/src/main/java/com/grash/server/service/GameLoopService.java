package com.grash.server.service;

import com.grash.server.domain.GameConstants;
import com.grash.server.domain.Player;
import com.grash.server.domain.Room;
import com.grash.server.dto.GameStateMessage;
import com.grash.server.dto.PlayerPositionDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Loop de jogo autoritativo: a cada tick lê o input mais recente de cada
 * jogador das salas em andamento, move as posições no servidor e
 * broadcasta o novo estado. O cliente nunca decide sua própria posição,
 * só envia teclas pressionadas — isso evita movimento client-authoritative
 * (fácil de trapacear) mesmo neste MVP simples.
 */
@Service
public class GameLoopService {

    private static final double DELTA_SECONDS = GameConstants.TICK_RATE_MS / 1000.0;
    /** Passes de resolução de colisão por tick — mais de uma suaviza aglomerados de 3+ jogadores. */
    private static final int COLLISION_ITERATIONS = 3;

    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AtomicLong tick = new AtomicLong();

    public GameLoopService(RoomService roomService, SimpMessagingTemplate messagingTemplate) {
        this.roomService = roomService;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedRate = GameConstants.TICK_RATE_MS)
    public void tick() {
        long currentTick = tick.incrementAndGet();
        for (Room room : roomService.getActiveGameRooms()) {
            List<Player> players = new ArrayList<>(room.getPlayers());
            players.forEach(this::movePlayer);

            for (int i = 0; i < COLLISION_ITERATIONS; i++) {
                resolvePlayerCollisions(players);
            }

            List<PlayerPositionDto> positions = players.stream()
                    .map(player -> new PlayerPositionDto(player.getId(), player.getNickname(), player.getX(), player.getY()))
                    .toList();

            messagingTemplate.convertAndSend(
                    "/topic/rooms/" + room.getId() + "/game",
                    new GameStateMessage(currentTick, positions)
            );
        }
    }

    private void movePlayer(Player player) {
        double dx = (player.isMoveRight() ? 1 : 0) - (player.isMoveLeft() ? 1 : 0);
        double dy = (player.isMoveDown() ? 1 : 0) - (player.isMoveUp() ? 1 : 0);

        if (dx == 0 && dy == 0) {
            return;
        }

        double length = Math.sqrt(dx * dx + dy * dy);
        double distance = GameConstants.PLAYER_SPEED_PER_SECOND * DELTA_SECONDS;
        double newX = player.getX() + (dx / length) * distance;
        double newY = player.getY() + (dy / length) * distance;

        player.setPosition(clampX(newX), clampY(newY));
    }

    /**
     * Empurra jogadores sobrepostos para fora um do outro (colisão círculo-círculo simples),
     * mantendo o resultado dentro dos limites da arena. Rodar em múltiplas passes por tick
     * evita que um terceiro jogador "roube" o espaço liberado por uma resolução anterior.
     */
    private void resolvePlayerCollisions(List<Player> players) {
        double minDistance = GameConstants.PLAYER_RADIUS * 2;

        for (int i = 0; i < players.size(); i++) {
            for (int j = i + 1; j < players.size(); j++) {
                Player a = players.get(i);
                Player b = players.get(j);

                double dx = b.getX() - a.getX();
                double dy = b.getY() - a.getY();
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance >= minDistance) {
                    continue;
                }

                double nx;
                double ny;
                if (distance < 1e-6) {
                    // posições idênticas (raro, ex.: spawn) — separa em uma direção arbitrária fixa
                    nx = 1;
                    ny = 0;
                } else {
                    nx = dx / distance;
                    ny = dy / distance;
                }

                double overlap = (minDistance - distance) / 2;
                a.setPosition(clampX(a.getX() - nx * overlap), clampY(a.getY() - ny * overlap));
                b.setPosition(clampX(b.getX() + nx * overlap), clampY(b.getY() + ny * overlap));
            }
        }
    }

    private double clampX(double x) {
        return clamp(x, GameConstants.PLAYER_RADIUS, GameConstants.ARENA_WIDTH - GameConstants.PLAYER_RADIUS);
    }

    private double clampY(double y) {
        return clamp(y, GameConstants.PLAYER_RADIUS, GameConstants.ARENA_HEIGHT - GameConstants.PLAYER_RADIUS);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
