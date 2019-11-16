package com.wnowakcraft.samples.restaurant.order.infrastructure;

import com.wnowakcraft.samples.restaurant.core.domain.logic.BusinessFlowDefinition;
import com.wnowakcraft.samples.restaurant.core.domain.logic.BusinessFlowProvisioner;
import com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.CommandChannelFactory;
import com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.CommandPublisher;
import com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.EventListenerBuilder;
import com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.EventListenerFactory;
import com.wnowakcraft.samples.restaurant.core.infrastructure.saga.BusinessFlowRunner;
import com.wnowakcraft.samples.restaurant.core.infrastructure.saga.BusinessFlowStateRepository;
import com.wnowakcraft.samples.restaurant.order.domain.logic.CreateOrderBusinessFlow.CreateOrderFlowState;
import com.wnowakcraft.samples.restaurant.order.domain.model.OrderCreatedEvent;
import com.wnowakcraft.samples.restaurant.order.domain.model.OrderEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateOrderBusinessFlowProvisioner implements BusinessFlowProvisioner<OrderEvent, CreateOrderFlowState> {
    private static final String CREATE_ORDER_RESPONSE_CHANNEL = "Restarurant_CreateOderCommandResponseChannel";
    @NonNull private final EventListenerBuilder eventListenerBuilder;
    @NonNull private final CommandChannelFactory commandChannelFactory;
    @NonNull private final EventListenerFactory eventListenerFactory;
    @NonNull private final BusinessFlowStateRepository<CreateOrderFlowState> businessFlowStateRepository;
    private CommandPublisher commandPublisher;
    private BusinessFlowRunner<OrderEvent, CreateOrderFlowState> businessFlowRunner;

    @Override
    public void provision(BusinessFlowDefinition<OrderEvent, CreateOrderFlowState> businessFlowDefinition) {
        commandChannelFactory
                .createMultiChannelCommandPublisher()
                .thenAccept(commandPublisher -> this.commandPublisher = commandPublisher);

        businessFlowRunner = BusinessFlowRunner
                .from(businessFlowDefinition)
                .withFlowStateProvider(businessFlowStateRepository::readFlowStateByCommandCorrelationId)
                .onInit((initialOrderFlowState, firstCommand) -> {
                    businessFlowStateRepository.save(initialOrderFlowState);
                    commandPublisher.send(firstCommand);

                })
                .onFlowStateChange((newOrderFlowState, nextCommand) -> {
                    businessFlowStateRepository.update(newOrderFlowState);
                    commandPublisher.send(nextCommand);

                })
                .onFlowFinished((orderFlowFinalState, finalizingOptionalCommand) -> {
                    businessFlowStateRepository.delete(orderFlowFinalState);
                    finalizingOptionalCommand.ifPresent(commandPublisher::send);

                })
                .run();

        eventListenerBuilder
                .listenToEventsOfKind(OrderEvent.class)
                .acceptOnly(OrderCreatedEvent.class)
                .onEvent(businessFlowRunner::onInitHandler)
                .listenToEvents();

        commandChannelFactory
                .createResponseChannel(CREATE_ORDER_RESPONSE_CHANNEL)
                .thenAccept(channel -> channel.invokeOnCommand(businessFlowRunner::onCommandResponse));

    }

}
