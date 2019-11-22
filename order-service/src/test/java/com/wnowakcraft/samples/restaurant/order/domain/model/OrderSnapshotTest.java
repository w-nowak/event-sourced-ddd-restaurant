package com.wnowakcraft.samples.restaurant.order.domain.model;

import com.google.common.testing.NullPointerTester;
import com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate.Version;
import com.wnowakcraft.samples.restaurant.core.domain.model.DomainBoundBusinessId;
import com.wnowakcraft.samples.restaurant.core.utils.ApplicationClock;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.wnowakcraft.samples.restaurant.core.utils.ApplicationClock.getFixedClockFor;
import static com.wnowakcraft.samples.restaurant.core.utils.ApplicationClock.getSystemDefaultClock;
import static org.assertj.core.api.Assertions.assertThat;

class OrderSnapshotTest {
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
    void createsNewOrderSnapshot() {
        var orderSnapshot = OrderSnapshot.newSnapshot(OrderModelTestData.ORDER_ID, OrderModelTestData.CUSTOMER_ID, OrderModelTestData.RESTAURANT_ID, Order.Status.APPROVED, OrderModelTestData.THREE_ORDER_ITEMS);

        assertThat(orderSnapshot.getAggregateId()).isEqualTo(OrderModelTestData.ORDER_ID);
        assertThat(orderSnapshot.getCustomerId()).isEqualTo(OrderModelTestData.CUSTOMER_ID);
        assertThat(orderSnapshot.getRestaurantId()).isEqualTo(OrderModelTestData.RESTAURANT_ID);
        assertThat(orderSnapshot.getOrderStatus()).isEqualTo(Order.Status.APPROVED);
        assertThat(orderSnapshot.getOrderItems()).isEqualTo(OrderModelTestData.THREE_ORDER_ITEMS);
        assertThat(orderSnapshot.getCreationDate()).isEqualTo(CURRENT_INSTANT);
        assertThat(orderSnapshot.getAggregateVersion()).isEqualTo(Version.NONE);
        assertThat(orderSnapshot.getSnapshotId()).isNotNull();
        assertThat(orderSnapshot.getSnapshotId().getValue()).startsWith("ORDER-ORDER-S-");
        assertThat(orderSnapshot.getSnapshotId().getValue()).matches(DomainBoundBusinessId.STRING_ID_REGEX);
    }

    @Test
    void recreatesOrderSnapshotFromData() {
        var orderSnapshot = OrderSnapshot.recreateFrom(
                OrderModelTestData.ORDER_SNAPSHOT_ID, OrderModelTestData.ORDER_ID, OrderModelTestData.CREATION_DATE, OrderModelTestData.AGGREGATE_VERSION, OrderModelTestData.CUSTOMER_ID,
                OrderModelTestData.RESTAURANT_ID, Order.Status.CANCELLED, OrderModelTestData.THREE_ORDER_ITEMS
        );

        assertThat(orderSnapshot.getAggregateId()).isEqualTo(OrderModelTestData.ORDER_ID);
        assertThat(orderSnapshot.getCustomerId()).isEqualTo(OrderModelTestData.CUSTOMER_ID);
        assertThat(orderSnapshot.getRestaurantId()).isEqualTo(OrderModelTestData.RESTAURANT_ID);
        assertThat(orderSnapshot.getOrderStatus()).isEqualTo(Order.Status.CANCELLED);
        assertThat(orderSnapshot.getOrderItems()).isEqualTo(OrderModelTestData.THREE_ORDER_ITEMS);
        assertThat(orderSnapshot.getCreationDate()).isEqualTo(OrderModelTestData.CREATION_DATE);
        assertThat(orderSnapshot.getAggregateVersion()).isEqualTo(OrderModelTestData.AGGREGATE_VERSION);
        assertThat(orderSnapshot.getSnapshotId()).isEqualTo(OrderModelTestData.ORDER_SNAPSHOT_ID);
    }

    @Test
    void verifiesEqualsAndHashCodeContract()
    {
        EqualsVerifier.forClass(OrderSnapshot.class).withRedefinedSuperclass().verify();
    }

    @Test
    void verifiesNullPointerContractOfPublicStaticMethods() {
        new NullPointerTester()
                .setDefault(Order.Id.class, OrderModelTestData.ORDER_ID)
                .setDefault(OrderSnapshot.Id.class, OrderModelTestData.ORDER_SNAPSHOT_ID)
                .setDefault(RestaurantId.class, OrderModelTestData.RESTAURANT_ID)
                .setDefault(CustomerId.class, OrderModelTestData.CUSTOMER_ID)
                .testAllPublicStaticMethods(OrderSnapshot.class);
    }
}