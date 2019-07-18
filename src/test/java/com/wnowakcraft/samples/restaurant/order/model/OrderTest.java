package com.wnowakcraft.samples.restaurant.order.model;

import com.google.common.testing.NullPointerTester;
import com.wnowakcraft.samples.restaurant.core.domain.Aggregate;
import com.wnowakcraft.samples.restaurant.core.utils.ApplicationClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static com.wnowakcraft.samples.restaurant.core.utils.ApplicationClock.getFixedClockFor;
import static com.wnowakcraft.samples.restaurant.core.utils.ApplicationClock.getSystemDefaultClock;
import static com.wnowakcraft.samples.restaurant.order.model.OrderModelTestData.*;
import static java.util.Collections.emptyList;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class OrderTest {
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
    void createsNewInstanceOfOrder() {
        final var newOrder = Order.newOrder(CUSTOMER_ID, RESTAURANT_ID, THREE_ORDER_ITEMS);

        assertThat(newOrder).isNotNull();
        assertThat(newOrder.getId()).isNotNull();
        assertThat(newOrder.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(newOrder.getRestaurantId()).isEqualTo(RESTAURANT_ID);
        assertThat(newOrder.getOrderItems()).isEqualTo(THREE_ORDER_ITEMS);
        assertThat(newOrder.getStatus()).isEqualTo(Order.Status.APPROVAL_PENDING);
        assertThat(newOrder.getVersion()).isEqualTo(Aggregate.Version.NONE);
        assertThat(newOrder.getChanges()).containsExactly(
                new OrderCreatedEvent(newOrder.getId(), CUSTOMER_ID, RESTAURANT_ID, THREE_ORDER_ITEMS)
        );
    }

    @Test
    void restoresOrderFromEvents_success() {
        var orderVersion = Aggregate.Version.of(10);
        var events = List.<OrderEvent>of(
                new OrderCreatedEvent(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, THREE_ORDER_ITEMS),
                new OrderApprovedEvent(ORDER_ID)
        );

        var restoredOrder = Order.restoreFrom(events, orderVersion);

        assertThat(restoredOrder).isNotNull();
        assertThat(restoredOrder.getId()).isNotNull();
        assertThat(restoredOrder.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(restoredOrder.getRestaurantId()).isEqualTo(RESTAURANT_ID);
        assertThat(restoredOrder.getOrderItems()).isEqualTo(THREE_ORDER_ITEMS);
        assertThat(restoredOrder.getStatus()).isEqualTo(Order.Status.APPROVED);
        assertThat(restoredOrder.getVersion()).isEqualTo(orderVersion);
        assertThat(restoredOrder.getChanges()).isEmpty();
    }

    @Test
    void restoresOrderFromEvents_whenFirstEventIsNotValid_throwsIllegalArgumentException() {
        var orderVersion = Aggregate.Version.of(10);
        var invalidOrderSequence = List.<OrderEvent>of(
                new OrderApprovedEvent(ORDER_ID),
                new OrderCreatedEvent(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, THREE_ORDER_ITEMS)
        );

        var expectedException = catchThrowable(() -> Order.restoreFrom(invalidOrderSequence, orderVersion));

        assertThat(expectedException).isNotNull();
        assertThat(expectedException).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void restoresOrderFromEvents_whenEventCollectionIsEmpty_throwsIllegalArgumentException() {
        var orderVersion = Aggregate.Version.of(10);

        var expectedException = catchThrowable(() -> Order.restoreFrom(emptyList(), orderVersion));

        assertThat(expectedException).isNotNull();
        assertThat(expectedException).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void restoresOrderFromEvents_whenWhenVersionIsNotSpecified_throwsIllegalArgumentException() {
        var expectedException = catchThrowable(() -> Order.restoreFrom(emptyList(), Aggregate.Version.NONE));

        assertThat(expectedException).isNotNull();
        assertThat(expectedException).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void restoresOrderFromSnapshotAndEvents_snapshotAndEventsAreGiven_successCreateOrderIsApproved() {
        var orderVersion = Aggregate.Version.of(10);
        var orderSnapshot = OrderSnapshot.recreateFrom(ORDER_SNAPSHOT_ID, ORDER_ID, CREATION_DATE, orderVersion,
                CUSTOMER_ID, RESTAURANT_ID, Order.Status.APPROVAL_PENDING, THREE_ORDER_ITEMS);
        var events = of(new OrderApprovedEvent(ORDER_ID));

        var restoredOrder = Order.restoreFrom(orderSnapshot, events, orderVersion);

        assertThat(restoredOrder).isNotNull();
        assertThat(restoredOrder.getId()).isNotNull();
        assertThat(restoredOrder.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(restoredOrder.getRestaurantId()).isEqualTo(RESTAURANT_ID);
        assertThat(restoredOrder.getOrderItems()).isEqualTo(THREE_ORDER_ITEMS);
        assertThat(restoredOrder.getStatus()).isEqualTo(Order.Status.APPROVED);
        assertThat(restoredOrder.getVersion()).isEqualTo(orderVersion);
        assertThat(restoredOrder.getChanges()).isEmpty();
    }

    @Test
    void restoresOrderFromSnapshotAndEvents_onlySnapshotIsGiven_successCreateOrderIsApprovalPending() {
        var orderVersion = Aggregate.Version.of(10);
        var orderSnapshot = OrderSnapshot.recreateFrom(ORDER_SNAPSHOT_ID, ORDER_ID, CREATION_DATE, orderVersion,
                CUSTOMER_ID, RESTAURANT_ID, Order.Status.APPROVAL_PENDING, THREE_ORDER_ITEMS);

        Collection<OrderEvent> noEvents = emptyList();
        var restoredOrder = Order.restoreFrom(orderSnapshot, noEvents, orderVersion);

        assertThat(restoredOrder).isNotNull();
        assertThat(restoredOrder.getId()).isNotNull();
        assertThat(restoredOrder.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(restoredOrder.getRestaurantId()).isEqualTo(RESTAURANT_ID);
        assertThat(restoredOrder.getOrderItems()).isEqualTo(THREE_ORDER_ITEMS);
        assertThat(restoredOrder.getStatus()).isEqualTo(Order.Status.APPROVAL_PENDING);
        assertThat(restoredOrder.getVersion()).isEqualTo(orderVersion);
        assertThat(restoredOrder.getChanges()).isEmpty();
    }

    @Test
    void restoresOrderFromSnapshotAndEvents_whenWhenVersionIsNotSpecified_throwsIllegalArgumentException() {
        var orderSnapshotVersion = Aggregate.Version.of(10);
        var orderSnapshot = OrderSnapshot.recreateFrom(ORDER_SNAPSHOT_ID, ORDER_ID, CREATION_DATE, orderSnapshotVersion,
                CUSTOMER_ID, RESTAURANT_ID, Order.Status.APPROVAL_PENDING, THREE_ORDER_ITEMS);

        Collection<OrderEvent> noEvents = emptyList();
        var expectedException = catchThrowable(() ->Order.restoreFrom(orderSnapshot, noEvents, Aggregate.Version.NONE));

        assertThat(expectedException).isNotNull();
        assertThat(expectedException).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancelOrder() {
        var orderVersion = Aggregate.Version.of(10);
        var orderCreated = of(new OrderCreatedEvent(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, THREE_ORDER_ITEMS));
        final var approvalPendingOrder = Order.restoreFrom(orderCreated, orderVersion);

        approvalPendingOrder.cancel();

        assertThat(approvalPendingOrder).isNotNull();
        assertThat(approvalPendingOrder.getId()).isNotNull();
        assertThat(approvalPendingOrder.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(approvalPendingOrder.getRestaurantId()).isEqualTo(RESTAURANT_ID);
        assertThat(approvalPendingOrder.getOrderItems()).isEqualTo(THREE_ORDER_ITEMS);
        assertThat(approvalPendingOrder.getStatus()).isEqualTo(Order.Status.CANCELLED);
        assertThat(approvalPendingOrder.getVersion()).isEqualTo(orderVersion);
        assertThat(approvalPendingOrder.getChanges()).containsExactly(new OrderCanceledEvent(ORDER_ID));
    }

    @Test
    void approveOrder() {
        var orderVersion = Aggregate.Version.of(10);
        var orderCreated = of(new OrderCreatedEvent(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, THREE_ORDER_ITEMS));
        final var approvalPendingOrder = Order.restoreFrom(orderCreated, orderVersion);

        approvalPendingOrder.approve();

        assertThat(approvalPendingOrder).isNotNull();
        assertThat(approvalPendingOrder.getId()).isNotNull();
        assertThat(approvalPendingOrder.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(approvalPendingOrder.getRestaurantId()).isEqualTo(RESTAURANT_ID);
        assertThat(approvalPendingOrder.getOrderItems()).isEqualTo(THREE_ORDER_ITEMS);
        assertThat(approvalPendingOrder.getStatus()).isEqualTo(Order.Status.APPROVED);
        assertThat(approvalPendingOrder.getVersion()).isEqualTo(orderVersion);
        assertThat(approvalPendingOrder.getChanges()).containsExactly(new OrderApprovedEvent(ORDER_ID));
    }

    @Test
    void verifiesNullPointerContractOfPublicInstanceMethods() {
        new NullPointerTester().testAllPublicInstanceMethods(ORDER);
    }

    @Test
    void verifiesNullPointerContractOfPublicStaticMethods() {
        new NullPointerTester()
                .setDefault(RestaurantId.class, RESTAURANT_ID)
                .setDefault(CustomerId.class, CUSTOMER_ID)
                .setDefault(OrderSnapshot.class, ORDER_SNAPSHOT)
                .setDefault(Collection.class, of(ORDER_CREATED_EVENT))
                .testAllPublicStaticMethods(Order.class);
    }
}