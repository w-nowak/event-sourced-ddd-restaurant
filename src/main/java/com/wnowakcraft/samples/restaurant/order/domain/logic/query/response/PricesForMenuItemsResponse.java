package com.wnowakcraft.samples.restaurant.order.domain.logic.query.response;

import com.wnowakcraft.samples.restaurant.core.domain.model.Response;
import com.wnowakcraft.samples.restaurant.order.domain.model.MenuItem;
import com.wnowakcraft.samples.restaurant.order.domain.model.MenuItemId;
import lombok.NonNull;
import lombok.Value;

import java.util.Map;

@Value
public class PricesForMenuItemsResponse implements Response {
    @NonNull private final Map<MenuItemId, MenuItem> menuItemsByMenuId;
}
