package com.wnowakcraft.samples.restaurant.order.model;

import com.wnowakcraft.samples.restaurant.core.domain.DomainBoundBusinessId;

public class RestaurantId extends DomainBoundBusinessId {
    private RestaurantId(String aggregateId, String domainName, String aggregateName, char aggregateTypeSymbol) {
        super(aggregateId, domainName, aggregateName, aggregateTypeSymbol);
    }

    public static RestaurantId of(String aggregateId)
    {
        return new RestaurantId(aggregateId, "RESTAURANT", "RESTAURANT", 'A');
    }
}
