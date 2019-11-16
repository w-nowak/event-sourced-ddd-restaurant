package com.wnowakcraft.samples.restaurant.core.infrastructure.messaging;

import com.wnowakcraft.samples.restaurant.core.domain.model.Command;

public interface CommandPublisher {
    void send(Command nextCommand);
}
