package com.wnowakcraft.samples.restaurant.order.domain.logic.query;

import com.wnowakcraft.samples.restaurant.core.domain.model.AbstractQuery;
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
public class ValidateOrderByCustomerQuery extends AbstractQuery {
    @NonNull private final CustomerId customerId;
    @NonNull private final Order.Id orderId;
    @NonNull private final Money orderTotal;
}
