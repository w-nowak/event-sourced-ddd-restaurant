package com.wnowakcraft.samples.restaurant.order.infrastructure.data.store;

import lombok.Value;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;

@Value
public class KafkaBrokerConfig {
    private final String bootstrapServers;

    @Inject
    public KafkaBrokerConfig(@ConfigProperty(name = "service.infrastructure.kafka.bootstrapServers") String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }
}
