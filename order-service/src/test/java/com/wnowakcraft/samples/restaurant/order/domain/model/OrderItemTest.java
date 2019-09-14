package com.wnowakcraft.samples.restaurant.order.domain.model;

import com.google.common.testing.NullPointerTester;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemTest {
    @Test
    void createsNewInstanceOfOrderItem() {
        var orderItem = new OrderItem(OrderModelTestData.ORDER_ITEM_1.QUANTITY, OrderModelTestData.ORDER_ITEM_1.NAME, OrderModelTestData.ORDER_ITEM_1.MENU_ITEM_ID);

        assertThat(orderItem.getQuantity()).isEqualTo(OrderModelTestData.ORDER_ITEM_1.QUANTITY);
        assertThat(orderItem.getName()).isEqualTo(OrderModelTestData.ORDER_ITEM_1.NAME);
        assertThat(orderItem.getMenuItemId()).isEqualTo(OrderModelTestData.ORDER_ITEM_1.MENU_ITEM_ID);
    }

    @Test
    void verifiesEqualsAndHashCodeContract() {
        EqualsVerifier.forClass(OrderItem.class).verify();
    }

    @Test
    void verifiesNullPointerContract() {
        new NullPointerTester()
                .setDefault(MenuItemId.class, OrderModelTestData.ORDER_ITEM_1.MENU_ITEM_ID)
                .testAllPublicConstructors(OrderItem.class);
    }
}