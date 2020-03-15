package com.wnowakcraft.samples.restaurant.order.domain.logic.query;

import com.wnowakcraft.samples.restaurant.core.domain.model.AbstractQuery;
import com.wnowakcraft.samples.restaurant.order.domain.model.MenuItemId;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

import java.util.Collection;

@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class GetPricesForMenuItemsQuery extends AbstractQuery {
    @NonNull private final Collection<MenuItemId> menuItemIds;
}
