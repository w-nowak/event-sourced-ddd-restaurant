package com.wnowakcraft.samples.restaurant.core.domain.logic;

import com.wnowakcraft.samples.restaurant.core.domain.model.Event;
import com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.CommandChannelFactory;
import com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.EventListenerBuilder;
import com.wnowakcraft.samples.restaurant.core.infrastructure.saga.BusinessFlowRunner;
import com.wnowakcraft.samples.restaurant.core.infrastructure.saga.BusinessFlowStateHandler;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
public class DefaultBusinessFlowProvisioner<E extends Event, S> implements BusinessFlowProvisioner<E, S> {
    @NonNull private final EventListenerBuilder eventListenerBuilder;
    @NonNull private final CommandChannelFactory commandChannelFactory;
    @NonNull private final BusinessFlowStateHandler<S> flowStateHandler;
    @NonNull private final BusinessFlowProvisionerConfig<E> flowProvisionerConfig;
    private BusinessFlowRunner<E, S> flowRunner;

    @Override
    public void provision(BusinessFlowDefinition<E, S> businessFlowDefinition) {

        flowRunner = BusinessFlowRunner
                .from(businessFlowDefinition)
                .withFlowStateProvider(flowStateHandler::readFlowState)
                .onInit(flowStateHandler::createNewState)
                .onFlowStateChange(flowStateHandler::updateState)
                .onFlowFinished(flowStateHandler::finalizeState)
                .run();

        eventListenerBuilder
                .<E>listenToEventsOfKind(flowProvisionerConfig.getEventKindToListenTo())
                .acceptOnly(flowProvisionerConfig.getEventInitializingFlow())
                .onEvent(flowRunner::onInitHandler)
                .listenToEvents();

        commandChannelFactory
                .createResponseChannel(flowProvisionerConfig.getCommandResponseChannelName())
                .thenAccept(channel -> channel.invokeOnCommand(flowRunner::onCommandResponse));
    }

    @Value
    public static class BusinessFlowProvisionerConfig<E extends Event> {
        @NonNull private final String commandResponseChannelName;
        @NonNull private final Class<? super E> eventKindToListenTo;
        @NonNull private final Class<? extends E> eventInitializingFlow;
    }
}
