package com.wnowakcraft.samples.restaurant.order.model;

import com.google.common.testing.NullPointerTester;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import static com.wnowakcraft.samples.restaurant.order.model.OrderModelTestData.ORDER_ITEM_1.*;
import static org.assertj.core.api.Assertions.assertThat;

class OrderItemTest {
    @Test
    void createsNewInstanceOfOrderItem() {
        var orderItem = new OrderItem(QUANTITY, NAME, MENU_ITEM_ID);

        assertThat(orderItem.getQuantity()).isEqualTo(QUANTITY);
        assertThat(orderItem.getName()).isEqualTo(NAME);
        assertThat(orderItem.getMenuItemId()).isEqualTo(MENU_ITEM_ID);
    }

    @Test
    void verifiesEqualsAndHashCodeContract() {
        EqualsVerifier.forClass(OrderItem.class).verify();
    }

    @Test
    void verifiesNullPointerContract() {
        new NullPointerTester()
                .setDefault(MenuItemId.class, MENU_ITEM_ID)
                .testAllPublicConstructors(OrderItem.class);
    }
}