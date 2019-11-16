package com.wnowakcraft.samples.restaurant.core.domain.model;

import java.util.UUID;

public interface Command extends Message {
    UUID getCorrelationId();
}
