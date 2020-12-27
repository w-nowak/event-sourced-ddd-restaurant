package com.wnowakcraft.samples.restaurant.core.domain.model;

public interface WithUpdatableVersion {
    void updateVersionTo(Aggregate.Version version);
}
