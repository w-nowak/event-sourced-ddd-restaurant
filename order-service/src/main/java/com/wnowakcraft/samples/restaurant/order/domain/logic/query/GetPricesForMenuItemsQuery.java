package com.wnowakcraft.samples.restaurant.order.domain.logic.query;

import com.wnowakcraft.samples.restaurant.core.domain.model.AbstractQuery;
import com.wnowakcraft.samples.restaurant.order.domain.model.MenuItemId;
import lombok.NonNull;
import lombok.Value;

import java.util.Collection;

@Value
public class GetPricesForMenuItemsQuery extends AbstractQuery {
    @NonNull private final Collection<MenuItemId> menuItemIds;
}
