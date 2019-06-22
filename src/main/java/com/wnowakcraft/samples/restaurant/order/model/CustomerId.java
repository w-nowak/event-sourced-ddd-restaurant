package com.wnowakcraft.samples.restaurant.order.model;

import com.wnowakcraft.samples.restaurant.core.domain.AbstractAggregate;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class CustomerId extends AbstractAggregate.PrefixedUuidAggregateId {
    private CustomerId(String domainName, String aggregateType, String aggregateId) {
        super(domainName, aggregateType, aggregateId);
    }

    public static CustomerId of(String aggregateId)
    {
        return new CustomerId("CUSTOMER", "CUSTOMER", aggregateId);
    }
}
