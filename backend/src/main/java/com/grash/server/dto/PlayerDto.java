package com.grash.server.dto;

import com.grash.server.domain.Player;

public record PlayerDto(String id, String nickname, String status) {

    public static PlayerDto from(Player player) {
        return new PlayerDto(player.getId(), player.getNickname(), player.getStatus().name());
    }
}
