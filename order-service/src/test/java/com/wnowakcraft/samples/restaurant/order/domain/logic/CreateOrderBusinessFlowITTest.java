package com.wnowakcraft.samples.restaurant.order.domain.logic;

import com.google.common.collect.ImmutableMap;
import com.wnowakcraft.samples.restaurant.core.domain.logic.BusinessFlowTestProvisioner;
import com.wnowakcraft.samples.restaurant.core.domain.logic.GivenBusinessFlowInteractions;
import com.wnowakcraft.samples.restaurant.core.domain.model.Money;
import com.wnowakcraft.samples.restaurant.core.domain.model.Response;
import com.wnowakcraft.samples.restaurant.order.domain.logic.CreateOrderBusinessFlow.CreateOrderFlowState;
import com.wnowakcraft.samples.restaurant.order.domain.logic.command.ApproveOrderCommand;
import com.wnowakcraft.samples.restaurant.order.domain.logic.command.AuthorizePaymentCommand;
import com.wnowakcraft.samples.restaurant.order.domain.logic.command.ConfirmKitchenTicketCommand;
import com.wnowakcraft.samples.restaurant.order.domain.logic.command.CreateKitchenTicketCommand;
import com.wnowakcraft.samples.restaurant.order.domain.logic.command.response.KitchenTicketConfirmedResponse;
import com.wnowakcraft.samples.restaurant.order.domain.logic.command.response.KitchenTicketCreatedResponse;
import com.wnowakcraft.samples.restaurant.order.domain.logic.command.response.OrderApprovedResponse;
import com.wnowakcraft.samples.restaurant.order.domain.logic.command.response.PaymentAuthorizedResponse;
import com.wnowakcraft.samples.restaurant.order.domain.logic.query.GetPricesForMenuItemsQuery;
import com.wnowakcraft.samples.restaurant.order.domain.logic.query.ValidateOrderByCustomerQuery;
import com.wnowakcraft.samples.restaurant.order.domain.logic.query.response.OrderValidatedResponse;
import com.wnowakcraft.samples.restaurant.order.domain.logic.query.response.PricesForMenuItemsResponse;
import com.wnowakcraft.samples.restaurant.order.domain.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.wnowakcraft.samples.restaurant.core.domain.logic.DefaultBusinessFlowProvisioner.*;
import static com.wnowakcraft.samples.restaurant.order.domain.model.OrderModelTestData.*;
import static java.lang.String.format;

class CreateOrderBusinessFlowITTest {
    private static final OrderCreatedEvent ORDER_CREATED_EVENT = new OrderCreatedEvent(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, THREE_ORDER_ITEMS);
    private static final GetPricesForMenuItemsQuery GET_PRICES_FOR_THREE_MENU_ITEMS_QUERY = createGetPricesForMenuItemsQueryFor(THREE_ORDER_ITEMS);
    private static final GivenBusinessFlowInteractions<OrderCreatedEvent> givenBusinessFlowInteractions = // @formatter:off
            GivenBusinessFlowInteractions
                    .givenBusinessFlowInitializedWith(ORDER_CREATED_EVENT)
                    .givenFollowingCommandTypeSent(GetPricesForMenuItemsQuery.class)
                        .thenRespondWith(followingPrices(GET_PRICES_FOR_THREE_MENU_ITEMS_QUERY, 25.9, 41.3, 7.0))
                    .givenFollowingCommandTypeSent(ValidateOrderByCustomerQuery.class)
                        .thenRespondWith(orderValidatedSuccessfullyResponse())
                    .givenFollowingCommandTypeSent(CreateKitchenTicketCommand.class)
                        .thenRespondWith(kitchenTicketCreatedSuccessfullyResponse(KITCHEN_TICKET_ID))
                    .givenFollowingCommandTypeSent(AuthorizePaymentCommand.class)
                        .thenRespondWith(paymentAuthorizedResponse())
                    .givenFollowingCommandTypeSent(ConfirmKitchenTicketCommand.class)
                        .thenRespondWith(kitchenTicketConfirmedResponse())
                    .givenFollowingCommandTypeSent(ApproveOrderCommand.class)
                        .thenRespondWith(orderApprovedResponse())
                    .whenFollowingResponseIsReceived(OrderApprovedResponse.class)
                        .thenFlowIsFinished();
    // @formatter:on

    private static Function<ApproveOrderCommand, Response> orderApprovedResponse() {
        return command -> new OrderApprovedTestResponse(command.getCorrelationId());
    }

    private static Function<ConfirmKitchenTicketCommand,Response> kitchenTicketConfirmedResponse() {
        return command -> new KitchenTicketConfirmedTestResponse(command.getCorrelationId());
    }

    private static Function<AuthorizePaymentCommand,Response> paymentAuthorizedResponse() {
        return command -> new PaymentAuthorizedTestResponse(command.getCorrelationId());
    }

    private static Function<CreateKitchenTicketCommand, Response> kitchenTicketCreatedSuccessfullyResponse(KitchenTicketId kitchenTicketId) {
        return command -> new KitchenTicketCreatedTestResponse(command.getCorrelationId(), kitchenTicketId);
    }

    private static Function<ValidateOrderByCustomerQuery, Response> orderValidatedSuccessfullyResponse() {
        return query -> new OrderValidatedTestResponse(query.getCorrelationId());
    }

    private static Response followingPrices(GetPricesForMenuItemsQuery query, double... pricesForItems) {
        return PricesForMenuItemsTestResponse.createFor(query, pricesForItems);
    }

    private static GetPricesForMenuItemsQuery createGetPricesForMenuItemsQueryFor(Collection<OrderItem> threeOrderItems) {
        Collection<MenuItemId> menuItemsPrices = threeOrderItems.stream().map(OrderItem::getMenuItemId).collect(Collectors.toUnmodifiableList());
        return new GetPricesForMenuItemsQuery(menuItemsPrices);
    }

    @Getter
    @RequiredArgsConstructor
    private static final class PricesForMenuItemsTestResponse implements PricesForMenuItemsResponse {
        private final Map<MenuItemId, MenuItem> menuItemsByMenuId;
        private final UUID correlationId;

        public static PricesForMenuItemsTestResponse createFor(GetPricesForMenuItemsQuery query, double... pricesForItems) {
            return new PricesForMenuItemsTestResponse(buildPricesForMenuItems(query, pricesForItems), query.getCorrelationId());
        }

        private static Map<MenuItemId, MenuItem> buildPricesForMenuItems(GetPricesForMenuItemsQuery query, double[] pricesForItems) {
            ImmutableMap.Builder<MenuItemId, MenuItem> pricesMapBuilder = ImmutableMap.builder();

            byte priceIndex = 0;
            for(MenuItemId menuItemId : query.getMenuItemIds()) {
                pricesMapBuilder.put(menuItemId, new MenuItem(format("Menu Item %d", priceIndex), ORDER_ITEM_1.MENU_ITEM_ID, Money.of(pricesForItems[priceIndex++])));
            }

            return pricesMapBuilder.build();
        }
    }

    @Getter
    @RequiredArgsConstructor
    private static final class OrderValidatedTestResponse implements OrderValidatedResponse {
        private final UUID correlationId;
    }

    @Getter
    @RequiredArgsConstructor
    private static final class KitchenTicketCreatedTestResponse implements KitchenTicketCreatedResponse {
        private final UUID correlationId;
        private final KitchenTicketId kitchenTicketId;
    }

    @Getter
    @RequiredArgsConstructor
    private static final class PaymentAuthorizedTestResponse implements PaymentAuthorizedResponse {
        private final UUID correlationId;
    }

    @Getter
    @RequiredArgsConstructor
    private static final class KitchenTicketConfirmedTestResponse implements KitchenTicketConfirmedResponse {
        private final UUID correlationId;
    }

    @Getter
    @RequiredArgsConstructor
    private static final class OrderApprovedTestResponse implements OrderApprovedResponse {
        private final UUID correlationId;
    }

    private BusinessFlowTestProvisioner<OrderCreatedEvent, CreateOrderFlowState> businessFlowTestProvisioner;

    @BeforeEach
    void setUp() {
        var config = new BusinessFlowProvisionerConfig<>("channelName", OrderEvent.class, OrderCreatedEvent.class);
        businessFlowTestProvisioner = new BusinessFlowTestProvisioner<>(givenBusinessFlowInteractions, config);
        new CreateOrderBusinessFlow(businessFlowTestProvisioner);
    }

    @Test
    void createOderBusinessFlowFinishesSuccessfully() {
        businessFlowTestProvisioner.triggerBusinessFlowInitEvent();
    }
}
