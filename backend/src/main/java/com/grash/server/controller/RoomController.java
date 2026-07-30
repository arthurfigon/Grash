package com.grash.server.controller;

import com.grash.server.domain.Player;
import com.grash.server.domain.Room;
import com.grash.server.dto.CreateRoomRequest;
import com.grash.server.dto.JoinRoomRequest;
import com.grash.server.dto.RoomStateMessage;
import com.grash.server.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<RoomStateMessage> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        Room room = roomService.createRoom(request.nickname());
        String playerId = room.getOwnerId();
        return ResponseEntity.ok(RoomStateMessage.of(room, playerId));
    }

    @PostMapping("/{code}/join")
    public ResponseEntity<RoomStateMessage> joinRoom(@PathVariable String code, @Valid @RequestBody JoinRoomRequest request) {
        Player player = roomService.joinRoomByCode(code, request.nickname());
        Room room = roomService.getRoomByCode(code);
        return ResponseEntity.ok(RoomStateMessage.of(room, player.getId()));
    }

    @GetMapping("/{code}")
    public ResponseEntity<RoomStateMessage> getRoom(@PathVariable String code) {
        Room room = roomService.getRoomByCode(code);
        return ResponseEntity.ok(RoomStateMessage.of(room));
    }
}
