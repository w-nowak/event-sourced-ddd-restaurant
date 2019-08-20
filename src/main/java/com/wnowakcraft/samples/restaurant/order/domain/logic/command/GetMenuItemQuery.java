package com.wnowakcraft.samples.restaurant.order.domain.logic.command;

import com.wnowakcraft.samples.restaurant.order.domain.model.MenuItemId;
import lombok.*;

@Value
public final class GetMenuItemQuery {
    @NonNull public final MenuItemId menuItemId;
}
