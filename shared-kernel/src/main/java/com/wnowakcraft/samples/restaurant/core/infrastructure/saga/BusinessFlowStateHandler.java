package com.wnowakcraft.samples.restaurant.core.infrastructure.saga;

import com.wnowakcraft.samples.restaurant.core.domain.model.Command;
import com.wnowakcraft.samples.restaurant.core.infrastructure.saga.BusinessFlowRunner.StateEnvelope;

import java.util.UUID;

public interface BusinessFlowStateHandler<S> {
    StateEnvelope<S> readFlowState(UUID commandCorrelationId);

    void createNewState(StateEnvelope<S> flowInitialState, Command initialCommand);

    void updateState(StateEnvelope<S> flowState, Command nextCommand);

    void finalizeState(StateEnvelope<S> flowState, Command flowFinalizingCommand);
}
