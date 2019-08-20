package com.wnowakcraft.samples.restaurant.order.domain.logic.command;

import com.wnowakcraft.samples.restaurant.core.domain.model.Command;
import com.wnowakcraft.samples.restaurant.order.domain.model.MenuItemId;
import com.wnowakcraft.samples.restaurant.order.domain.model.Order;
import com.wnowakcraft.samples.restaurant.order.domain.model.OrderItem;
import com.wnowakcraft.samples.restaurant.order.domain.model.RestaurantId;
import lombok.NonNull;
import lombok.Value;

import java.util.Collection;

@Value
public class CreateKitchenTicketCommand implements Command {
    @NonNull private final Order.Id orderId;
    @NonNull private final RestaurantId restaurantId;
    @NonNull private final Collection<OrderItem> menuItemId;

}
