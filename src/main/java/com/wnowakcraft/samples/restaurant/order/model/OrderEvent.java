package com.wnowakcraft.samples.restaurant.order.model;

import com.wnowakcraft.samples.restaurant.core.domain.Event;
import com.wnowakcraft.samples.restaurant.order.model.Order.OrderId;


public interface OrderEvent extends Event<OrderId> {

    void applyOn(Order order);
}