package com.wnowakcraft.samples.restaurant.order.application;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class OrderLine {
    private final String menuItemId;
    private final String name;
    private final int quantity;
}
