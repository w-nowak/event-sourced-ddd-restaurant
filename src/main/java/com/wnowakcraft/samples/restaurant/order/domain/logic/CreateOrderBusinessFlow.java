package com.wnowakcraft.samples.restaurant.order.domain.logic;

import com.wnowakcraft.samples.restaurant.core.domain.logic.BusinessFlowProvisioner;
import com.wnowakcraft.samples.restaurant.core.domain.logic.BusinessFlow;
import com.wnowakcraft.samples.restaurant.core.domain.model.Money;
import com.wnowakcraft.samples.restaurant.order.domain.logic.command.*;
import com.wnowakcraft.samples.restaurant.order.domain.logic.command.response.KitchenTicketCreatedResponse;
import com.wnowakcraft.samples.restaurant.order.domain.logic.command.response.KitchenTicketCreationFailedResponse;
import com.wnowakcraft.samples.restaurant.order.domain.logic.command.response.OrderValidatedResponse;
import com.wnowakcraft.samples.restaurant.order.domain.logic.command.response.OrderValidationFailedResponse;
import com.wnowakcraft.samples.restaurant.order.domain.model.OrderCreatedEvent;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;
import static com.wnowakcraft.samples.restaurant.core.domain.logic.BusinessFlow.OnResponse.failureWithCompensation;
import static com.wnowakcraft.samples.restaurant.core.domain.logic.BusinessFlow.OnResponse.success;

public class CreateOrderBusinessFlow {
    private final BusinessFlow<OrderCreatedEvent> createOrderBusinessFlow =
            BusinessFlow
                .startWith(OrderCreatedEvent.class)
                    .compensateBy(e -> new RejectOrderCommand(e.getConcernedAggregateId()))
                .thenSend(e ->validateOrderCommandFor(e))
                    .on(OrderValidatedResponse.class, success())
                    .on(OrderValidationFailedResponse.class, failureWithCompensation())
                .thenSend(e -> createKitchenTicketCommandOf(e))
                    .on(KitchenTicketCreatedResponse.class, success())
                    .on(KitchenTicketCreationFailedResponse.class, failureWithCompensation())
                    .compensateBy(e -> new CancelKitchenTicketCommand())
                .done();

    public CreateOrderBusinessFlow(BusinessFlowProvisioner<OrderCreatedEvent> businessFlowProvisioner) {
        requireNonNull(businessFlowProvisioner, "businessFlowProvisioner");

        businessFlowProvisioner.provision(createOrderBusinessFlow);
    }

    private ValidateOrderByCustomerCommand validateOrderCommandFor(OrderCreatedEvent event) {
        return new ValidateOrderByCustomerCommand(event.getCustomerId(), event.getConcernedAggregateId(), new Money(20));
    }

    private CreateKitchenTicketCommand createKitchenTicketCommandOf(OrderCreatedEvent event) {
        return new CreateKitchenTicketCommand(event.getConcernedAggregateId(), event.getRestaurantId(), event.getOrderItems());
    }
}
