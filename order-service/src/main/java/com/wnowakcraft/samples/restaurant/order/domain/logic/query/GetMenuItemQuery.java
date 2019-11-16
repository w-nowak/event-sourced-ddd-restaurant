package com.wnowakcraft.samples.restaurant.order.domain.logic.query;

import com.wnowakcraft.samples.restaurant.core.domain.model.AbstractQuery;
import com.wnowakcraft.samples.restaurant.order.domain.model.MenuItemId;
import lombok.NonNull;
import lombok.Value;

@Value
public final class GetMenuItemQuery extends AbstractQuery {
    @NonNull public final MenuItemId menuItemId;
}
