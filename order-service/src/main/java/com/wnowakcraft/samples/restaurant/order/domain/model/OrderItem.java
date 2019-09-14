package com.wnowakcraft.samples.restaurant.order.domain.model;

import lombok.*;

@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public final class OrderItem {
    private final int quantity;
    @NonNull private final String name;
    @NonNull private final MenuItemId menuItemId;
}
