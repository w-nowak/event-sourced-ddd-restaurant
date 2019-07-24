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

class OrderCancelledEventTest {
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
    void restoresOrderCanceledEventFromData() {
        var orderCanceledEvent = OrderCancelledEvent.restoreFrom(ORDER_ID, SEQUENCE_NUMBER, CREATION_DATE);

        assertThat(orderCanceledEvent).isNotNull();
        assertThat(orderCanceledEvent.getConcernedAggregateId()).isEqualTo(ORDER_ID);
        assertThat(orderCanceledEvent.getSequenceNumber()).isEqualTo(SEQUENCE_NUMBER);
        assertThat(orderCanceledEvent.getConcernedAggregateId()).isEqualTo(ORDER_ID);
        assertThat(orderCanceledEvent.getGeneratedOn()).isEqualTo(CREATION_DATE);
    }

    @Test
    void createsNewOrderCanceledEvent() {
        var orderCanceledEvent = new OrderCancelledEvent(ORDER_ID);

        assertThat(orderCanceledEvent).isNotNull();
        assertThat(orderCanceledEvent.getConcernedAggregateId()).isEqualTo(ORDER_ID);
        assertThat(orderCanceledEvent.getSequenceNumber()).isEqualTo(SequenceNumber.NOT_ASSIGNED);
        assertThat(orderCanceledEvent.getConcernedAggregateId()).isEqualTo(ORDER_ID);
        assertThat(orderCanceledEvent.getGeneratedOn()).isEqualTo(CURRENT_INSTANT);
    }

    @Test
    void applyOn_shouldDelegateToApplyMethodOfPassedOrder() {
        final var orderCanceledEvent = new OrderCancelledEvent(ORDER_ID);
        final var order = mock(Order.class);

        orderCanceledEvent.applyOn(order);

        then(order).should().apply(orderCanceledEvent);
    }

    @Test
    void verifiesEqualsAndHashCodeContract() {
        EqualsVerifier
                .forClass(OrderCancelledEvent.class)
                .withPrefabValues(SequenceNumber.class, SequenceNumber.of(15), SequenceNumber.of(5))
                .verify();
    }

    @Test
    void verifiesNullPointerContractOfPublicInstanceMethods() {
        new NullPointerTester().testAllPublicInstanceMethods(new OrderCancelledEvent(ORDER_ID));
    }

    @Test
    void verifiesNullPointerContractOfPublicConstructors() {
        new NullPointerTester()
                .setDefault(Order.Id.class, ORDER_ID)
                .testAllPublicConstructors(OrderCancelledEvent.class);
    }

    @Test
    void verifiesNullPointerContractOfPublicStaticMethods() {
        new NullPointerTester()
                .setDefault(Order.Id.class, ORDER_ID)
                .setDefault(SequenceNumber.class, SEQUENCE_NUMBER)
                .testAllPublicStaticMethods(OrderCancelledEvent.class);
    }
}