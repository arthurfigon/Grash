package com.grash.server.dto;

import com.grash.server.domain.ClueEntry;

public record ClueEntryDto(String playerId, int lap, String text) {

    public static ClueEntryDto from(ClueEntry entry) {
        return new ClueEntryDto(entry.playerId(), entry.lap(), entry.text());
    }
}
