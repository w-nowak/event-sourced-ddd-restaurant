package com.wnowakcraft.samples.restaurant.order.model;

import com.google.common.testing.NullPointerTester;
import com.wnowakcraft.samples.restaurant.core.domain.Event;
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

class OrderApprovedEventTest {
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
    void restoresOrderApprovedEventFromData() {
        var orderApprovedEvent = OrderApprovedEvent.restoreFrom(ORDER_ID, SEQUENCE_NUMBER, CREATION_DATE);

        assertThat(orderApprovedEvent).isNotNull();
        assertThat(orderApprovedEvent.getConcernedAggregateId()).isEqualTo(ORDER_ID);
        assertThat(orderApprovedEvent.getSequenceNumber()).isEqualTo(SEQUENCE_NUMBER);
        assertThat(orderApprovedEvent.getConcernedAggregateId()).isEqualTo(ORDER_ID);
        assertThat(orderApprovedEvent.getGeneratedOn()).isEqualTo(CREATION_DATE);
    }

    @Test
    void createsNewOrderApprovedEvent() {
        var orderApprovedEvent = new OrderApprovedEvent(ORDER_ID);

        assertThat(orderApprovedEvent).isNotNull();
        assertThat(orderApprovedEvent.getConcernedAggregateId()).isEqualTo(ORDER_ID);
        assertThat(orderApprovedEvent.getSequenceNumber()).isEqualTo(Event.SequenceNumber.NOT_ASSIGNED);
        assertThat(orderApprovedEvent.getConcernedAggregateId()).isEqualTo(ORDER_ID);
        assertThat(orderApprovedEvent.getGeneratedOn()).isEqualTo(CURRENT_INSTANT);
    }

    @Test
    void applyOn_shouldDelegateToApplyMethodOfPassedOrder() {
        final var orderApprovedEvent = new OrderApprovedEvent(ORDER_ID);
        final var order = mock(Order.class);

        orderApprovedEvent.applyOn(order);

        then(order).should().apply(orderApprovedEvent);
    }

    @Test
    void verifiesEqualsAndHashCodeContract() {
        EqualsVerifier
                .forClass(OrderApprovedEvent.class)
                .withPrefabValues(Event.SequenceNumber.class, Event.SequenceNumber.of(15), Event.SequenceNumber.of(5))
                .verify();
    }

    @Test
    void verifiesNullPointerContractOfPublicInstanceMethods() {
        new NullPointerTester().testAllPublicInstanceMethods(new OrderApprovedEvent(ORDER_ID));
    }

    @Test
    void verifiesNullPointerContractOfPublicConstructors() {
        new NullPointerTester()
                .setDefault(Order.Id.class, ORDER_ID)
                .testAllPublicConstructors(OrderApprovedEvent.class);
    }

    @Test
    void verifiesNullPointerContractOfPublicStaticMethods() {
        new NullPointerTester()
                .setDefault(Order.Id.class, ORDER_ID)
                .setDefault(Event.SequenceNumber.class, SEQUENCE_NUMBER)
                .testAllPublicStaticMethods(OrderApprovedEvent.class);
    }
}