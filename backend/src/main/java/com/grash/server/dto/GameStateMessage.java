package com.grash.server.dto;

import java.util.List;

public record GameStateMessage(long tick, List<PlayerPositionDto> players) {
}
