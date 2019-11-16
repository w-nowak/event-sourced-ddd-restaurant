package com.wnowakcraft.samples.restaurant.order.domain.logic.query.response;

import com.wnowakcraft.samples.restaurant.core.domain.model.Response;
import com.wnowakcraft.samples.restaurant.order.domain.model.MenuItem;
import com.wnowakcraft.samples.restaurant.order.domain.model.MenuItemId;

import java.util.Map;

public interface PricesForMenuItemsResponse extends Response {
    Map<MenuItemId, MenuItem> getMenuItemsByMenuId();
}
