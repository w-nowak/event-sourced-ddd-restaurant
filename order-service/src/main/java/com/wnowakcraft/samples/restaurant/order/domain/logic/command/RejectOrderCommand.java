package com.wnowakcraft.samples.restaurant.order.domain.logic.command;

import com.wnowakcraft.samples.restaurant.core.domain.model.Command;
import com.wnowakcraft.samples.restaurant.order.domain.model.Order;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RejectOrderCommand implements Command {
    @NonNull private final Order.Id orderId;
}
