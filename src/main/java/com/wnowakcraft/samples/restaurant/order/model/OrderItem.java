package com.wnowakcraft.samples.restaurant.order.model;

import lombok.*;

@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public final class OrderItem {
    @NonNull private final int quantity;
    @NonNull private final String name;
    @NonNull private final MenuItemId menuItemId;
}
