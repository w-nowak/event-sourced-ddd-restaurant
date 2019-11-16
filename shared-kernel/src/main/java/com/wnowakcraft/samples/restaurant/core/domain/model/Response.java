package com.wnowakcraft.samples.restaurant.core.domain.model;

import java.util.UUID;

public interface Response extends Message {
    UUID getCorrelationId();
}
