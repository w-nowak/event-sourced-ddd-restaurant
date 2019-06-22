package com.wnowakcraft.samples.restaurant.order.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class OrderItem {
    private final int quantity;
    private final String name;
    private final MenuItemId menuItemId;
}
