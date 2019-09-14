package com.wnowakcraft.samples.restaurant.order.domain.model;

import com.wnowakcraft.samples.restaurant.core.domain.model.Money;
import lombok.NonNull;
import lombok.Value;

@Value
public class MenuItem {
    @NonNull private final String name;
    @NonNull private final MenuItemId menuItemId;
    @NonNull private final Money price;
}
