package com.wnowakcraft.samples.restaurant.order.application;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class OrderLine {
    private final long menuItemId;
    private final long quantity;
}
