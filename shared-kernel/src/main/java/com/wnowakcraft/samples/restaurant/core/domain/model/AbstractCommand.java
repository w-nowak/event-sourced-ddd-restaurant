package com.wnowakcraft.samples.restaurant.core.domain.model;

import lombok.Getter;

import java.util.UUID;

@Getter
public class AbstractCommand implements Command {
    private final UUID correlationId;

    protected AbstractCommand() {
        this.correlationId = UUID.randomUUID();
    }
}
