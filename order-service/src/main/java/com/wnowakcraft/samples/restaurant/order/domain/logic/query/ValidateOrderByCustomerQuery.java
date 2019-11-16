package com.wnowakcraft.samples.restaurant.order.domain.logic.query;

import com.wnowakcraft.samples.restaurant.core.domain.model.AbstractQuery;
import com.wnowakcraft.samples.restaurant.core.domain.model.Money;
import com.wnowakcraft.samples.restaurant.order.domain.model.CustomerId;
import com.wnowakcraft.samples.restaurant.order.domain.model.Order;
import lombok.NonNull;
import lombok.Value;

@Value
public class ValidateOrderByCustomerQuery extends AbstractQuery {
    @NonNull private final CustomerId customerId;
    @NonNull private final Order.Id orderId;
    @NonNull private final Money orderTotal;
}
