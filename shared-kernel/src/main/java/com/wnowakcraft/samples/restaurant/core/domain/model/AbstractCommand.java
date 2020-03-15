package com.wnowakcraft.samples.restaurant.core.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Getter
@ToString
@EqualsAndHashCode
public class AbstractCommand implements Command {
    private final UUID correlationId;

    protected AbstractCommand() {
        this.correlationId = UUID.randomUUID();
    }
}
