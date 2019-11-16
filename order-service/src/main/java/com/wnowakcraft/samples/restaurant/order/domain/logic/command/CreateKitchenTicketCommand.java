package com.wnowakcraft.samples.restaurant.order.domain.logic.command;

import com.wnowakcraft.samples.restaurant.core.domain.model.AbstractCommand;
import com.wnowakcraft.samples.restaurant.order.domain.model.Order;
import com.wnowakcraft.samples.restaurant.order.domain.model.OrderItem;
import com.wnowakcraft.samples.restaurant.order.domain.model.RestaurantId;
import lombok.NonNull;
import lombok.Value;

import java.util.Collection;

@Value
public class CreateKitchenTicketCommand extends AbstractCommand {
    @NonNull private final Order.Id orderId;
    @NonNull private final RestaurantId restaurantId;
    @NonNull private final Collection<OrderItem> menuItemId;

}
