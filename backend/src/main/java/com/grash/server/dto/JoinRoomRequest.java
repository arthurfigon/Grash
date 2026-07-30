package com.grash.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinRoomRequest(
        @NotBlank(message = "nickname é obrigatório")
        @Size(min = 2, max = 20, message = "nickname deve ter entre 2 e 20 caracteres")
        String nickname
) {
}
