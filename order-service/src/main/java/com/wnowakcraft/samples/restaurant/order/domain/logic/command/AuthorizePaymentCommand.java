package com.wnowakcraft.samples.restaurant.order.domain.logic.command;

import com.wnowakcraft.samples.restaurant.core.domain.model.AbstractCommand;
import com.wnowakcraft.samples.restaurant.core.domain.model.Money;
import com.wnowakcraft.samples.restaurant.order.domain.model.CustomerId;
import com.wnowakcraft.samples.restaurant.order.domain.model.Order;
import lombok.NonNull;
import lombok.Value;

@Value
public class AuthorizePaymentCommand extends AbstractCommand {
    @NonNull private final Order.Id orderId;
    @NonNull private final CustomerId customerId;
    @NonNull private final Money orderTotal;
}
