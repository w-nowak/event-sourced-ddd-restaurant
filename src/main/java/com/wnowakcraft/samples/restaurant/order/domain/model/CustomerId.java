package com.wnowakcraft.samples.restaurant.order.domain.model;

import com.wnowakcraft.samples.restaurant.core.domain.model.DomainBoundBusinessId;

public final class CustomerId extends DomainBoundBusinessId {
    private CustomerId(String aggregateId, String domainName, String aggregateName, char aggregateTypeSymbol) {
        super(aggregateId, domainName, aggregateName, aggregateTypeSymbol);
    }

    public static CustomerId of(String aggregateId)
    {
        return new CustomerId(aggregateId, "CUSTOMER", "CUSTOMER", 'A');
    }
}
