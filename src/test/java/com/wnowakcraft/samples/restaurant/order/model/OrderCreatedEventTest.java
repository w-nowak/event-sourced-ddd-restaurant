package com.wnowakcraft.samples.restaurant.order.model;

import com.google.common.testing.NullPointerTester;
import com.wnowakcraft.samples.restaurant.core.domain.Event.SequenceNumber;
import com.wnowakcraft.samples.restaurant.core.utils.ApplicationClock;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.wnowakcraft.samples.restaurant.core.utils.ApplicationClock.getFixedClockFor;
import static com.wnowakcraft.samples.restaurant.core.utils.ApplicationClock.getSystemDefaultClock;
import static com.wnowakcraft.samples.restaurant.order.model.OrderModelTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class OrderCreatedEventTest {
    private static final Instant CURRENT_INSTANT = Instant.now();

    @BeforeEach
    void setUp() {
        ApplicationClock.setCurrentClock(getFixedClockFor(CURRENT_INSTANT));
    }

    @AfterEach
    void cleanUp() {
        ApplicationClock.setCurrentClock(getSystemDefaultClock());
    }

    @Test
    void restoresOrderCreatedEventFromData() {
        var orderCreatedEvent = OrderCreatedEvent
                .restoreFrom(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, THREE_ORDER_ITEMS, SEQUENCE_NUMBER, CREATION_DATE);

        assertThat(orderCreatedEvent).isNotNull();
        assertThat(orderCreatedEvent.getConcernedAggregateId()).isEqualTo(ORDER_ID);
        assertThat(orderCreatedEvent.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(orderCreatedEvent.getRestaurantId()).isEqualTo(RESTAURANT_ID);
        assertThat(orderCreatedEvent.getOrderItems()).isEqualTo(THREE_ORDER_ITEMS);
        assertThat(orderCreatedEvent.getSequenceNumber()).isEqualTo(SEQUENCE_NUMBER);
        assertThat(orderCreatedEvent.getConcernedAggregateId()).isEqualTo(ORDER_ID);
        assertThat(orderCreatedEvent.getGeneratedOn()).isEqualTo(CREATION_DATE);
    }

    @Test
    void createsNewOrderCreatedEvent() {
        var orderCreatedEvent = new OrderCreatedEvent(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, THREE_ORDER_ITEMS);

        assertThat(orderCreatedEvent).isNotNull();
        assertThat(orderCreatedEvent.getConcernedAggregateId()).isEqualTo(ORDER_ID);
        assertThat(orderCreatedEvent.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(orderCreatedEvent.getRestaurantId()).isEqualTo(RESTAURANT_ID);
        assertThat(orderCreatedEvent.getOrderItems()).isEqualTo(THREE_ORDER_ITEMS);
        assertThat(orderCreatedEvent.getSequenceNumber()).isEqualTo(SequenceNumber.NOT_ASSIGNED);
        assertThat(orderCreatedEvent.getConcernedAggregateId()).isEqualTo(ORDER_ID);
        assertThat(orderCreatedEvent.getGeneratedOn()).isEqualTo(CURRENT_INSTANT);
    }

    @Test
    void applyOn_shouldDelegateToApplyMethodOfPassedOrder() {
        final var orderCreatedEvent = new OrderCreatedEvent(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, THREE_ORDER_ITEMS);
        final var order = mock(Order.class);

        orderCreatedEvent.applyOn(order);

        then(order).should().apply(orderCreatedEvent);
    }

    @Test
    void verifiesEqualsAndHashCodeContract() {
        EqualsVerifier
                .forClass(OrderCreatedEvent.class)
                .withRedefinedSuperclass()
                .withPrefabValues(SequenceNumber.class, SequenceNumber.of(15), SequenceNumber.of(5))
                .verify();
    }

    @Test
    void verifiesNullPointerContractOfPublicConstructors() {
        new NullPointerTester()
                .setDefault(CustomerId.class, CUSTOMER_ID)
                .setDefault(RestaurantId.class, RESTAURANT_ID)
                .setDefault(Order.Id.class, ORDER_ID)
                .testAllPublicConstructors(OrderCreatedEvent.class);
    }

    @Test
    void verifiesNullPointerContractOfPublicStaticMethods() {
        new NullPointerTester()
                .setDefault(CustomerId.class, CUSTOMER_ID)
                .setDefault(RestaurantId.class, RESTAURANT_ID)
                .setDefault(Order.Id.class, ORDER_ID)
                .setDefault(SequenceNumber.class, SEQUENCE_NUMBER)
                .testAllPublicStaticMethods(OrderCreatedEvent.class);
    }

    @Test
    void verifiesNullPointerContractOfPublicInstanceMethods() {
        final var orderCreatedEvent = new OrderCreatedEvent(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, THREE_ORDER_ITEMS);
        new NullPointerTester().testAllPublicInstanceMethods(orderCreatedEvent);
    }
}