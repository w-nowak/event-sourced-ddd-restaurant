package com.wnowakcraft.samples.restaurant.order.domain.model;

import com.wnowakcraft.samples.restaurant.core.domain.model.Event;


public interface OrderEvent extends Event<Order.Id> {

    void applyOn(Order order);
}