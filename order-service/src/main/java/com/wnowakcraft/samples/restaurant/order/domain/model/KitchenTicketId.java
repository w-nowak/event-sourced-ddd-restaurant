package com.wnowakcraft.samples.restaurant.order.domain.model;

import com.wnowakcraft.samples.restaurant.core.domain.model.DomainBoundBusinessId;

public final class KitchenTicketId extends DomainBoundBusinessId {
    private KitchenTicketId(String aggregateId, String domainName, String aggregateName, char aggregateTypeSymbol) {
        super(aggregateId, domainName, aggregateName, aggregateTypeSymbol);
    }

    public static KitchenTicketId of(String aggregateId)
    {
        return new KitchenTicketId(aggregateId, "KITCHEN", "KITCHEN_TICKET", 'A');
    }
}
