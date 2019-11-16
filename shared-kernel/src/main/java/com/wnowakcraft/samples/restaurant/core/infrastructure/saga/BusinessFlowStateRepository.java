package com.wnowakcraft.samples.restaurant.core.infrastructure.saga;

import com.wnowakcraft.samples.restaurant.core.infrastructure.saga.BusinessFlowRunner.StateEnvelope;

import java.util.UUID;

public interface BusinessFlowStateRepository<S> {
    StateEnvelope<S> readFlowStateByCommandCorrelationId(UUID commandCorrelationId);

    void save(StateEnvelope<S> initialFlowState);

    void update(StateEnvelope<S> flowState);

    void delete(StateEnvelope<S> flowState);
}
