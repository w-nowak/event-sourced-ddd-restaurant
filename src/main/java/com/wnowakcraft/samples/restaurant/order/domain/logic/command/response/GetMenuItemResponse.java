package com.wnowakcraft.samples.restaurant.order.domain.logic.command.response;

import com.wnowakcraft.samples.restaurant.core.domain.model.Command;
import com.wnowakcraft.samples.restaurant.core.domain.model.Money;
import com.wnowakcraft.samples.restaurant.order.domain.model.MenuItemId;
import lombok.NonNull;
import lombok.Value;

@Value
public class GetMenuItemResponse implements Command {
    @NonNull private final MenuItemId menuItemId;
    @NonNull private final String name;
    @NonNull private final String description;
    @NonNull private final Money price;
}
