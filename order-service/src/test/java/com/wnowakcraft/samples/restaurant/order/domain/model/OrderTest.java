package com.wnowakcraft.samples.restaurant.order.domain.model;

import com.google.common.testing.NullPointerTester;
import com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate;
import com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate.Version;
import com.wnowakcraft.samples.restaurant.core.utils.ApplicationClock;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static com.wnowakcraft.samples.restaurant.core.utils.ApplicationClock.getFixedClockFor;
import static com.wnowakcraft.samples.restaurant.core.utils.ApplicationClock.getSystemDefaultClock;
import static com.wnowakcraft.samples.restaurant.order.domain.model.OrderModelTestData.*;
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
        Assertions.assertThat(newOrder.getOrderItems()).isEqualTo(THREE_ORDER_ITEMS);
        assertThat(newOrder.getStatus()).isEqualTo(Order.Status.APPROVAL_PENDING);
        assertThat(newOrder.getVersion()).isEqualTo(Version.NONE);
        assertThat(newOrder.getChanges()).containsExactly(
                new OrderCreatedEvent(newOrder.getId(), CUSTOMER_ID, RESTAURANT_ID, THREE_ORDER_ITEMS)
        );
    }

    @Test
    void restoresOrderFromEvents_success() {
        var events = List.<OrderEvent>of(
                new OrderCreatedEvent(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, THREE_ORDER_ITEMS),
                new OrderApprovedEvent(ORDER_ID)
        );

        var restoredOrder = Order.restoreFrom(events, ORDER_VERSION);

        assertThat(restoredOrder).isNotNull();
        assertThat(restoredOrder.getId()).isNotNull();
        assertThat(restoredOrder.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(restoredOrder.getRestaurantId()).isEqualTo(RESTAURANT_ID);
        Assertions.assertThat(restoredOrder.getOrderItems()).isEqualTo(THREE_ORDER_ITEMS);
        assertThat(restoredOrder.getStatus()).isEqualTo(Order.Status.APPROVED);
        assertThat(restoredOrder.getVersion()).isEqualTo(ORDER_VERSION);
        assertThat(restoredOrder.getChanges()).isEmpty();
    }

    @Test
    void restoresOrderFromEvents_whenFirstEventIsNotValid_throwsIllegalArgumentException() {
        var invalidOrderSequence = List.<OrderEvent>of(
                new OrderApprovedEvent(ORDER_ID),
                new OrderCreatedEvent(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, THREE_ORDER_ITEMS)
        );

        var expectedException = catchThrowable(() -> Order.restoreFrom(invalidOrderSequence, ORDER_VERSION));

        assertThat(expectedException).isNotNull();
        assertThat(expectedException).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void restoresOrderFromEvents_whenEventCollectionIsEmpty_throwsIllegalArgumentException() {
        var expectedException = catchThrowable(() -> Order.restoreFrom(emptyList(), ORDER_VERSION));

        assertThat(expectedException).isNotNull();
        assertThat(expectedException).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void restoresOrderFromEvents_whenWhenVersionIsNotSpecified_throwsIllegalArgumentException() {
        var expectedException = catchThrowable(() -> Order.restoreFrom(emptyList(), Version.NONE));

        assertThat(expectedException).isNotNull();
        assertThat(expectedException).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void restoresOrderFromSnapshotAndEvents_snapshotAndEventsAreGiven_successCreateOrderIsApproved() {
        var orderSnapshot = OrderSnapshot.recreateFrom(ORDER_SNAPSHOT_ID, ORDER_ID, CREATION_DATE, ORDER_VERSION,
                CUSTOMER_ID, RESTAURANT_ID, Order.Status.APPROVAL_PENDING, THREE_ORDER_ITEMS);
        var events = of(new OrderApprovedEvent(ORDER_ID));

        var restoredOrder = Order.restoreFrom(orderSnapshot, events, ORDER_VERSION);

        assertThat(restoredOrder).isNotNull();
        assertThat(restoredOrder.getId()).isNotNull();
        assertThat(restoredOrder.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(restoredOrder.getRestaurantId()).isEqualTo(RESTAURANT_ID);
        Assertions.assertThat(restoredOrder.getOrderItems()).isEqualTo(THREE_ORDER_ITEMS);
        assertThat(restoredOrder.getStatus()).isEqualTo(Order.Status.APPROVED);
        assertThat(restoredOrder.getVersion()).isEqualTo(ORDER_VERSION);
        assertThat(restoredOrder.getChanges()).isEmpty();
    }

    @Test
    void restoresOrderFromSnapshotAndEvents_onlySnapshotIsGiven_successCreateOrderIsApprovalPending() {
        var orderSnapshot = OrderSnapshot.recreateFrom(ORDER_SNAPSHOT_ID, ORDER_ID, CREATION_DATE, ORDER_VERSION,
                CUSTOMER_ID, RESTAURANT_ID, Order.Status.APPROVAL_PENDING, THREE_ORDER_ITEMS);

        Collection<OrderEvent> noEvents = emptyList();
        var restoredOrder = Order.restoreFrom(orderSnapshot, noEvents, ORDER_VERSION);

        assertThat(restoredOrder).isNotNull();
        assertThat(restoredOrder.getId()).isNotNull();
        assertThat(restoredOrder.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(restoredOrder.getRestaurantId()).isEqualTo(RESTAURANT_ID);
        Assertions.assertThat(restoredOrder.getOrderItems()).isEqualTo(THREE_ORDER_ITEMS);
        assertThat(restoredOrder.getStatus()).isEqualTo(Order.Status.APPROVAL_PENDING);
        assertThat(restoredOrder.getVersion()).isEqualTo(ORDER_VERSION);
        assertThat(restoredOrder.getChanges()).isEmpty();
    }

    @Test
    void restoresOrderFromSnapshotAndEvents_whenWhenVersionIsNotSpecified_throwsIllegalArgumentException() {
        var orderSnapshot = OrderSnapshot.recreateFrom(ORDER_SNAPSHOT_ID, ORDER_ID, CREATION_DATE, ORDER_VERSION,
                CUSTOMER_ID, RESTAURANT_ID, Order.Status.APPROVAL_PENDING, THREE_ORDER_ITEMS);

        Collection<OrderEvent> noEvents = emptyList();
        var expectedException = catchThrowable(() ->Order.restoreFrom(orderSnapshot, noEvents, Version.NONE));

        assertThat(expectedException).isNotNull();
        assertThat(expectedException).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void approveOrder_whenInApprovalPendingState_success() {
        var orderCreatedEvent = of(new OrderCreatedEvent(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, THREE_ORDER_ITEMS));
        final var order = Order.restoreFrom(orderCreatedEvent, ORDER_VERSION);

        order.approve();

        assertThat(order).isNotNull();
        assertThat(order.getId()).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(order.getRestaurantId()).isEqualTo(RESTAURANT_ID);
        Assertions.assertThat(order.getOrderItems()).isEqualTo(THREE_ORDER_ITEMS);
        assertThat(order.getStatus()).isEqualTo(Order.Status.APPROVED);
        assertThat(order.getVersion()).isEqualTo(ORDER_VERSION);
        assertThat(order.getChanges()).containsExactly(new OrderApprovedEvent(ORDER_ID));
    }

    @DisplayName("Order should not change state during approval when")
    @ParameterizedTest(name = "in {0} state")
    @MethodSource({"orderApproved"})
    void approveOrder_whenInApprovedState_shouldNotChangedState(Order.Status orderStatus, Collection<OrderEvent> orderEvents) {
        final var order = Order.restoreFrom(orderEvents, ORDER_VERSION);

        order.approve();

        assertThat(order).isNotNull();
        assertThat(order.getId()).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(order.getRestaurantId()).isEqualTo(RESTAURANT_ID);
        Assertions.assertThat(order.getOrderItems()).isEqualTo(THREE_ORDER_ITEMS);
        assertThat(order.getStatus()).isEqualTo(orderStatus);
        assertThat(order.getVersion()).isEqualTo(ORDER_VERSION);
        assertThat(order.getChanges()).isEmpty();
    }

    @DisplayName("Order cannot be approved when")
    @ParameterizedTest(name = "in {0} state")
    @MethodSource({"orderRejected", "orderCancelStarted", "orderCancelled"})
    void approveOrder_whenInInvalidState_failure(Order.Status orderStatus, Collection<OrderEvent> orderEvents) {
        final var order = Order.restoreFrom(orderEvents, ORDER_VERSION);

        var actualException = catchThrowable(order::approve);

        assertThat(actualException).isNotNull();
        assertThat(actualException).isInstanceOf(Aggregate.IllegalStateChangeException.class);
        var actualIllegalStateChangeException = (Aggregate.IllegalStateChangeException)actualException;
        assertThat(actualIllegalStateChangeException.getFromState()).isEqualTo(orderStatus);
        assertThat(actualIllegalStateChangeException.getToState()).isEqualTo(Order.Status.APPROVED);
    }

    @Test
    void cancelOrder_whenInApprovedState_success() {
        var orderCreatedEvent = of(
                new OrderCreatedEvent(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, THREE_ORDER_ITEMS),
                new OrderApprovedEvent(ORDER_ID)
        );
        final var order = Order.restoreFrom(orderCreatedEvent, ORDER_VERSION);

        order.cancel();

        assertThat(order).isNotNull();
        assertThat(order.getId()).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(order.getRestaurantId()).isEqualTo(RESTAURANT_ID);
        Assertions.assertThat(order.getOrderItems()).isEqualTo(THREE_ORDER_ITEMS);
        assertThat(order.getStatus()).isEqualTo(Order.Status.CANCEL_PENDING);
        assertThat(order.getVersion()).isEqualTo(ORDER_VERSION);
        assertThat(order.getChanges()).containsExactly(new OrderCancelStartedEvent(ORDER_ID));
    }

    @DisplayName("Order should not change state during cancellation when")
    @ParameterizedTest(name = "in {0} state")
    @MethodSource({"orderCancelStarted", "orderCancelled"})
    void cancelOrder_whenInCancellationState_shouldNotChangeState(Order.Status orderStatus, Collection<OrderEvent> orderEvents) {
        final var order = Order.restoreFrom(orderEvents, ORDER_VERSION);

        order.cancel();

        assertThat(order).isNotNull();
        assertThat(order.getId()).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(order.getRestaurantId()).isEqualTo(RESTAURANT_ID);
        Assertions.assertThat(order.getOrderItems()).isEqualTo(THREE_ORDER_ITEMS);
        assertThat(order.getStatus()).isEqualTo(orderStatus);
        assertThat(order.getVersion()).isEqualTo(ORDER_VERSION);
        assertThat(order.getChanges()).isEmpty();
    }

    @DisplayName("Order cannot be cancelled when")
    @ParameterizedTest(name = "in {0} state")
    @MethodSource({"orderCreated", "orderRejected"})
    void cancelOrder_whenInInvalidState_failure(Order.Status orderStatus, Collection<OrderEvent> orderEvents) {
        final var order = Order.restoreFrom(orderEvents, ORDER_VERSION);

        var actualException = catchThrowable(order::cancel);

        assertThat(actualException).isNotNull();
        assertThat(actualException).isInstanceOf(Aggregate.IllegalStateChangeException.class);
        var actualIllegalStateChangeException = (Aggregate.IllegalStateChangeException)actualException;
        assertThat(actualIllegalStateChangeException.getFromState()).isEqualTo(orderStatus);
        assertThat(actualIllegalStateChangeException.getToState()).isEqualTo(Order.Status.CANCEL_PENDING);
    }

    @Test
    void cancelConfirmed_whenInCancelStartedState_success() {
        var orderCancelStartedEvents = of(
                new OrderCreatedEvent(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, THREE_ORDER_ITEMS),
                new OrderApprovedEvent(ORDER_ID),
                new OrderCancelStartedEvent(ORDER_ID)
        );
        final var order = Order.restoreFrom(orderCancelStartedEvents, ORDER_VERSION);

        order.cancelConfirmed();

        assertThat(order).isNotNull();
        assertThat(order.getId()).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(order.getRestaurantId()).isEqualTo(RESTAURANT_ID);
        Assertions.assertThat(order.getOrderItems()).isEqualTo(THREE_ORDER_ITEMS);
        assertThat(order.getStatus()).isEqualTo(Order.Status.CANCELLED);
        assertThat(order.getVersion()).isEqualTo(ORDER_VERSION);
        assertThat(order.getChanges()).containsExactly(new OrderCancelledEvent(ORDER_ID));
    }

    @DisplayName("Order should not change state during cancel confirmation when")
    @ParameterizedTest(name = "in {0} state")
    @MethodSource({"orderCancelled"})
    void cancelConfirmed_whenInCancellationState_shouldNotChangeState(Order.Status orderStatus, Collection<OrderEvent> orderEvents) {
        final var order = Order.restoreFrom(orderEvents, ORDER_VERSION);

        order.cancelConfirmed();

        assertThat(order).isNotNull();
        assertThat(order.getId()).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(order.getRestaurantId()).isEqualTo(RESTAURANT_ID);
        Assertions.assertThat(order.getOrderItems()).isEqualTo(THREE_ORDER_ITEMS);
        assertThat(order.getStatus()).isEqualTo(orderStatus);
        assertThat(order.getVersion()).isEqualTo(ORDER_VERSION);
        assertThat(order.getChanges()).isEmpty();
    }

    @DisplayName("Order cancellation cannot be confirmed when")
    @ParameterizedTest(name = "in {0} state")
    @MethodSource({"orderCreated", "orderApproved", "orderRejected"})
    void cancelConfirmed_whenInInvalidState_failure(Order.Status orderStatus, Collection<OrderEvent> orderEvents) {
        final var order = Order.restoreFrom(orderEvents, ORDER_VERSION);

        var actualException = catchThrowable(order::cancelConfirmed);

        assertThat(actualException).isNotNull();
        assertThat(actualException).isInstanceOf(Aggregate.IllegalStateChangeException.class);
        var actualIllegalStateChangeException = (Aggregate.IllegalStateChangeException)actualException;
        assertThat(actualIllegalStateChangeException.getFromState()).isEqualTo(orderStatus);
        assertThat(actualIllegalStateChangeException.getToState()).isEqualTo(Order.Status.CANCELLED);
    }

    @Test
    void rejectOrder_whenInApprovalPendingState_success() {
        var orderCreatedEvent = of(new OrderCreatedEvent(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, THREE_ORDER_ITEMS));
        final var order = Order.restoreFrom(orderCreatedEvent, ORDER_VERSION);

        order.reject();

        assertThat(order).isNotNull();
        assertThat(order.getId()).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(order.getRestaurantId()).isEqualTo(RESTAURANT_ID);
        Assertions.assertThat(order.getOrderItems()).isEqualTo(THREE_ORDER_ITEMS);
        assertThat(order.getStatus()).isEqualTo(Order.Status.REJECTED);
        assertThat(order.getVersion()).isEqualTo(ORDER_VERSION);
        assertThat(order.getChanges()).containsExactly(new OrderRejectedEvent(ORDER_ID));
    }

    @DisplayName("Order should not change state during rejection when")
    @ParameterizedTest(name = "in {0} state")
    @MethodSource({"orderRejected"})
    void rejectOrder_whenInRejectedState_shouldNotChangedState(Order.Status orderStatus, Collection<OrderEvent> orderEvents) {
        final var order = Order.restoreFrom(orderEvents, ORDER_VERSION);

        order.reject();

        assertThat(order).isNotNull();
        assertThat(order.getId()).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(order.getRestaurantId()).isEqualTo(RESTAURANT_ID);
        Assertions.assertThat(order.getOrderItems()).isEqualTo(THREE_ORDER_ITEMS);
        assertThat(order.getStatus()).isEqualTo(orderStatus);
        assertThat(order.getVersion()).isEqualTo(ORDER_VERSION);
        assertThat(order.getChanges()).isEmpty();
    }

    @DisplayName("Order cannot be rejected when")
    @ParameterizedTest(name = "in {0} state")
    @MethodSource({"orderApproved", "orderCancelStarted", "orderCancelled"})
    void rejectOrder_whenInInvalidState_failure(Order.Status orderStatus, Collection<OrderEvent> orderEvents) {
        final var order = Order.restoreFrom(orderEvents, ORDER_VERSION);

        var actualException = catchThrowable(order::reject);

        assertThat(actualException).isNotNull();
        assertThat(actualException).isInstanceOf(Aggregate.IllegalStateChangeException.class);
        var actualIllegalStateChangeException = (Aggregate.IllegalStateChangeException)actualException;
        assertThat(actualIllegalStateChangeException.getFromState()).isEqualTo(orderStatus);
        assertThat(actualIllegalStateChangeException.getToState()).isEqualTo(Order.Status.REJECTED);
    }

    private static Stream<? extends Arguments> orderCreated() {
        return Stream.of(
                Arguments.of(
                        Order.Status.APPROVAL_PENDING,
                        List.of(ORDER_CREATED_EVENT)
                )
        );
    }

    private static Stream<? extends Arguments> orderApproved() {
        return Stream.of(
                Arguments.of(
                        Order.Status.APPROVED,
                        List.of(ORDER_CREATED_EVENT, ORDER_APPROVED_EVENT)
                )
        );
    }

    private static Stream<? extends Arguments> orderRejected() {
        return Stream.of(
                Arguments.of(
                        Order.Status.REJECTED,
                        List.of(ORDER_CREATED_EVENT, ORDER_REJECTED_EVENT)
                )
        );
    }

    private static Stream<? extends Arguments> orderCancelStarted() {
        return Stream.of(
                Arguments.of(
                        Order.Status.CANCEL_PENDING,
                        List.of(ORDER_CREATED_EVENT, ORDER_CANCEL_STARTED_EVENT)
                )
        );
    }

    private static Stream<? extends Arguments> orderCancelled() {
        return Stream.of(
                Arguments.of(
                        Order.Status.CANCELLED,
                        List.of(ORDER_CREATED_EVENT, ORDER_CANCEL_STARTED_EVENT, ORDER_CANCELLED_EVENT)
                )
        );
    }

    @Test
    void takeSnapshotOfOrderRetunsSnapshotWithCorrectState() {
        var events = List.<OrderEvent>of(
                new OrderCreatedEvent(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, THREE_ORDER_ITEMS),
                new OrderApprovedEvent(ORDER_ID)
        );
        var approvedOrder = Order.restoreFrom(events, AGGREGATE_VERSION);

        var approvedOrderSnapshot = approvedOrder.takeSnapshot();

        assertThat(approvedOrderSnapshot).isNotNull();
        assertThat(approvedOrderSnapshot.getSnapshotId()).isNotNull();
        assertThat(approvedOrderSnapshot.getAggregateId()).isEqualTo(ORDER_ID);
        assertThat(approvedOrderSnapshot.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(approvedOrderSnapshot.getRestaurantId()).isEqualTo(RESTAURANT_ID);
        assertThat(approvedOrderSnapshot.getCreationDate()).isEqualTo(CURRENT_INSTANT);
        assertThat(approvedOrderSnapshot.getOrderItems()).isEqualTo(THREE_ORDER_ITEMS);
        assertThat(approvedOrderSnapshot.getOrderStatus()).isEqualTo(Order.Status.APPROVED);
        assertThat(approvedOrderSnapshot.getAggregateVersion()).isEqualTo(AGGREGATE_VERSION);
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