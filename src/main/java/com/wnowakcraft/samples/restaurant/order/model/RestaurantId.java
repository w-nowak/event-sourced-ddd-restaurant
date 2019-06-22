package com.wnowakcraft.samples.restaurant.order.model;

import com.wnowakcraft.samples.restaurant.core.domain.AbstractAggregate;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RestaurantId extends AbstractAggregate.PrefixedUuidAggregateId {
    private RestaurantId(String domainName, String aggregateType, String aggregateId) {
        super(domainName, aggregateType, aggregateId);
    }

    public static RestaurantId of(String aggregateId)
    {
        return new RestaurantId("RESTAURANT", "RESTAURANT", aggregateId);
    }
}
