package com.wnowakcraft.samples.restaurant.order.domain.logic.command;

import com.wnowakcraft.samples.restaurant.core.domain.model.AbstractCommand;
import com.wnowakcraft.samples.restaurant.core.domain.model.Money;
import com.wnowakcraft.samples.restaurant.order.domain.model.CustomerId;
import com.wnowakcraft.samples.restaurant.order.domain.model.Order;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class AuthorizePaymentCommand extends AbstractCommand {
    @NonNull private final Order.Id orderId;
    @NonNull private final CustomerId customerId;
    @NonNull private final Money orderTotal;
}
