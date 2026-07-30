package com.grash.server.dto;

public record InputMessage(String playerId, boolean up, boolean down, boolean left, boolean right) {
}
