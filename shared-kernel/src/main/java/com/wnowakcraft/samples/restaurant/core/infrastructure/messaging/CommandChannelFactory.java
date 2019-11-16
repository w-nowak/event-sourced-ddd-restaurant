package com.wnowakcraft.samples.restaurant.core.infrastructure.messaging;

import java.util.concurrent.CompletableFuture;

public interface CommandChannelFactory {
    CompletableFuture<CommandChannel> createCommandChannel(String channelName);
    CompletableFuture<CommandPublisher> createMultiChannelCommandPublisher();
    CompletableFuture<CommandResponseChannel> createResponseChannel(String channelName);
}
