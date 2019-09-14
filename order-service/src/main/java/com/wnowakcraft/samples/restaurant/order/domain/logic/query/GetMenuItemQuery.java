package com.wnowakcraft.samples.restaurant.order.domain.logic.query;

import com.wnowakcraft.samples.restaurant.core.domain.model.Query;
import com.wnowakcraft.samples.restaurant.order.domain.model.MenuItemId;
import lombok.*;

@Value
public final class GetMenuItemQuery implements Query {
    @NonNull public final MenuItemId menuItemId;
}
