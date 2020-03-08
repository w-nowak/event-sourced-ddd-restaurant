package com.wnowakcraft.samples.restaurant.order.domain.logic;

import com.wnowakcraft.samples.restaurant.core.domain.logic.BusinessFlowDefinition;
import com.wnowakcraft.samples.restaurant.core.domain.logic.BusinessFlowProvisioner;
import com.wnowakcraft.samples.restaurant.core.domain.model.Money;
import com.wnowakcraft.samples.restaurant.order.domain.logic.command.*;
import com.wnowakcraft.samples.restaurant.order.domain.logic.command.response.*;
import com.wnowakcraft.samples.restaurant.order.domain.logic.query.GetPricesForMenuItemsQuery;
import com.wnowakcraft.samples.restaurant.order.domain.logic.query.ValidateOrderByCustomerQuery;
import com.wnowakcraft.samples.restaurant.order.domain.logic.query.response.OrderValidatedResponse;
import com.wnowakcraft.samples.restaurant.order.domain.logic.query.response.OrderValidationFailedResponse;
import com.wnowakcraft.samples.restaurant.order.domain.logic.query.response.PricesForMenuItemsResponse;
import com.wnowakcraft.samples.restaurant.order.domain.model.*;
import lombok.Getter;

import java.util.Collection;
import java.util.Map;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;
import static com.wnowakcraft.preconditions.Preconditions.requireStateThat;
import static com.wnowakcraft.samples.restaurant.core.domain.logic.BusinessFlowDefinition.OnResponse.*;
import static java.util.stream.Collectors.toUnmodifiableList;

public class CreateOrderBusinessFlow {
    private final BusinessFlowDefinition<OrderCreatedEvent, CreateOrderFlowState> createOrderBusinessFlowDefinition = // @formatter:off
            BusinessFlowDefinition
                .startWith(OrderCreatedEvent.class, CreateOrderFlowState::new)
                    .compensateBy(s -> new RejectOrderCommand(s.getOrderId()))

                .thenSend(CreateOrderBusinessFlow::getItemsCurrentPricesQuery)
                    .on(PricesForMenuItemsResponse.class, CreateOrderBusinessFlow::calculateOrderTotalForMenuItems)

                .thenSend(CreateOrderBusinessFlow::validateOrderByCustomerQuery)
                    .on(OrderValidatedResponse.class, CreateOrderBusinessFlow::orderValidated)
                    .on(OrderValidationFailedResponse.class, failureWithCompensation())

                .thenSend(CreateOrderBusinessFlow::createKitchenTicketCommand)
                    .on(KitchenTicketCreatedResponse.class, CreateOrderBusinessFlow::kitchenTicketCreated)
                    .on(KitchenTicketCreationFailedResponse.class, failureWithCompensation())
                    .compensateBy(s -> new CancelKitchenTicketCommand(s.getKitchenTicketId()))

                .thenSend(CreateOrderBusinessFlow::authorizePaymentCommand)
                    .on(PaymentAuthorizedResponse.class, success())
                    .on(PaymentAuthorizationFailedResponse.class, failureWithCompensation())

                .thenSend(CreateOrderBusinessFlow::confirmKitchenTicketCommand)
                    .on(KitchenTicketConfirmedResponse.class, success())
                    .on(KitchenTicketConfirmationFailedResponse.class, failureWithRetry())

                .thenSend(CreateOrderBusinessFlow::approveOrderCommand)
                    .on(OrderApprovedResponse.class, success())
                    .on(OrderApproveFailedResponse.class, failureWithRetry())
                .done();
    // @formatter:on

    public CreateOrderBusinessFlow(BusinessFlowProvisioner<OrderCreatedEvent, CreateOrderFlowState> businessFlowProvisioner) {
        requireNonNull(businessFlowProvisioner, "businessFlowProvisioner");

        businessFlowProvisioner.provision(createOrderBusinessFlowDefinition);
    }

    private static GetPricesForMenuItemsQuery getItemsCurrentPricesQuery(CreateOrderFlowState state) {
        return new GetPricesForMenuItemsQuery(
                state.getOrderItems().stream()
                        .map(OrderItem::getMenuItemId)
                        .collect(toUnmodifiableList())
        );
    }

    private static void calculateOrderTotalForMenuItems(PricesForMenuItemsResponse response, CreateOrderFlowState state) {
        state.addOrderTotal(sumAllPricesFor(response.getMenuItemsByMenuId()));
    }

    private static Money sumAllPricesFor(Map<MenuItemId, MenuItem> menuItemsById) {
        return menuItemsById.values().stream()
                .map(MenuItem::getPrice)
                .reduce(Money.ZERO, Money::add);
    }

    private static ValidateOrderByCustomerQuery validateOrderByCustomerQuery(CreateOrderFlowState state) {
        return new ValidateOrderByCustomerQuery(state.getCustomerId(), state.getOrderId(), state.getOrderTotal());
    }

    private static void orderValidated(OrderValidatedResponse response, CreateOrderFlowState state) {
        state.validated();
    }

    private static CreateKitchenTicketCommand createKitchenTicketCommand(CreateOrderFlowState state) {
        return new CreateKitchenTicketCommand(state.getOrderId(), state.getRestaurantId(), state.getOrderItems());
    }

    private static void kitchenTicketCreated(KitchenTicketCreatedResponse response, CreateOrderFlowState state) {
        requireStateThat(state.isValidated(),
                "Order needs to be validated against customer before can be proceeded with");
        state.kitchenTicketId(response.getKitchenTicketId());
    }

    private static ConfirmKitchenTicketCommand confirmKitchenTicketCommand(CreateOrderFlowState state) {
        requireStateThat(state.getKitchenTicketId() != null,
                "Kitchen ticket cannot be confirmed until it gets id assigned");
        return new ConfirmKitchenTicketCommand(state.getKitchenTicketId());
    }

    private static AuthorizePaymentCommand authorizePaymentCommand(CreateOrderFlowState state) {
        return new AuthorizePaymentCommand(state.getOrderId(), state.getCustomerId(), state.getOrderTotal());
    }

    private static ApproveOrderCommand approveOrderCommand(CreateOrderFlowState state) {
        return new ApproveOrderCommand(state.getOrderId());
    }

    @Getter
    public static class CreateOrderFlowState {
        private final Order.Id orderId;
        private final CustomerId customerId;
        private final RestaurantId restaurantId;
        private final Collection<OrderItem> orderItems;
        private Money orderTotal;
        private boolean validated;
        private KitchenTicketId kitchenTicketId;

        private CreateOrderFlowState(OrderCreatedEvent orderCreatedEvent) {
            this(orderCreatedEvent.getConcernedAggregateId(), orderCreatedEvent.getCustomerId(),
                    orderCreatedEvent.getRestaurantId(), orderCreatedEvent.getOrderItems());
        }

        public CreateOrderFlowState(Order.Id orderId, CustomerId customerId, RestaurantId restaurantId, Collection<OrderItem> orderItems) {
            this.orderId = requireNonNull(orderId, "orderId");
            this.customerId = requireNonNull(customerId, "customerId");
            this.restaurantId = requireNonNull(restaurantId , "restaurantId");
            this.orderItems = requireNonNull(orderItems, "orderItems");
        }

        public void addOrderTotal(Money orderTotal) {
            this.orderTotal = requireNonNull(orderTotal, "orderTotal");
        }

        public void validated() {
            validated = true;
        }

        public void kitchenTicketId(KitchenTicketId kitchenTicketId) {
            this.kitchenTicketId = requireNonNull(kitchenTicketId, "kitchenTicketId");
        }
    }
}
