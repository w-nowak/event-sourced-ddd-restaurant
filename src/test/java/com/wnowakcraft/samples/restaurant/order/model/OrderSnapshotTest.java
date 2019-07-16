package com.wnowakcraft.samples.restaurant.order.model;

import com.google.common.testing.NullPointerTester;
import com.wnowakcraft.samples.restaurant.core.domain.Aggregate.Version;
import com.wnowakcraft.samples.restaurant.core.domain.DomainBoundBusinessId;
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
        var orderSnapshot = OrderSnapshot.newSnapshot(ORDER_ID, RESTAURANT_ID, Order.Status.APPROVED, THREE_ORDER_ITEMS);

        assertThat(orderSnapshot.getAggregateId()).isEqualTo(ORDER_ID);
        assertThat(orderSnapshot.getRestaurantId()).isEqualTo(RESTAURANT_ID);
        assertThat(orderSnapshot.getOrderStatus()).isEqualTo(Order.Status.APPROVED);
        assertThat(orderSnapshot.getOrderItems()).isEqualTo(THREE_ORDER_ITEMS);
        assertThat(orderSnapshot.getCreationDate()).isEqualTo(CURRENT_INSTANT);
        assertThat(orderSnapshot.getAggregateVersion()).isEqualTo(Version.NONE);
        assertThat(orderSnapshot.getSnapshotId()).isNotNull();
        assertThat(orderSnapshot.getSnapshotId().getId()).startsWith("ORDER-ORDER-S-");
        assertThat(orderSnapshot.getSnapshotId().getId()).matches(DomainBoundBusinessId.STRING_ID_REGEX);
    }

    @Test
    void recreatesOrderSnapshotFromData() {
        var orderSnapshot = OrderSnapshot.recreateFrom(
                ORDER_SNAPSHOT_ID, ORDER_ID, CREATION_DATE, AGGREGATE_VERSION, RESTAURANT_ID,
                Order.Status.CANCELLED, THREE_ORDER_ITEMS
        );

        assertThat(orderSnapshot.getAggregateId()).isEqualTo(ORDER_ID);
        assertThat(orderSnapshot.getRestaurantId()).isEqualTo(RESTAURANT_ID);
        assertThat(orderSnapshot.getOrderStatus()).isEqualTo(Order.Status.CANCELLED);
        assertThat(orderSnapshot.getOrderItems()).isEqualTo(THREE_ORDER_ITEMS);
        assertThat(orderSnapshot.getCreationDate()).isEqualTo(CREATION_DATE);
        assertThat(orderSnapshot.getAggregateVersion()).isEqualTo(AGGREGATE_VERSION);
        assertThat(orderSnapshot.getSnapshotId()).isEqualTo(ORDER_SNAPSHOT_ID);
    }

    @Test
    void verifiesEqualsAndHashCodeContract()
    {
        EqualsVerifier.forClass(OrderSnapshot.class).withRedefinedSuperclass().verify();
    }

    @Test
    void verifiesNullPointerContractOfPublicStaticMethods() {
        new NullPointerTester()
                .setDefault(Order.Id.class, ORDER_ID)
                .setDefault(OrderSnapshot.Id.class, ORDER_SNAPSHOT_ID)
                .setDefault(RestaurantId.class, RESTAURANT_ID)
                .testAllPublicStaticMethods(OrderSnapshot.class);
    }
}