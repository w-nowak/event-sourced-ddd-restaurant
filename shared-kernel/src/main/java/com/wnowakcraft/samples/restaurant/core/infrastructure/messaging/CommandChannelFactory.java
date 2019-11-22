package com.wnowakcraft.samples.restaurant.core.infrastructure.messaging;

import java.util.concurrent.CompletableFuture;

public interface CommandChannelFactory {
    CompletableFuture<CommandResponseChannel> createResponseChannel(String channelName);
}
