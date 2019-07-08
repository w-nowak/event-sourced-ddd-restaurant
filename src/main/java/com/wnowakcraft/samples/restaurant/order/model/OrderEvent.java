package com.wnowakcraft.samples.restaurant.order.model;

import com.wnowakcraft.samples.restaurant.core.domain.Event;


public interface OrderEvent extends Event<Order.Id> {

    void applyOn(Order order);
}