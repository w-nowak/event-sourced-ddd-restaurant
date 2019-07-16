package com.wnowakcraft.samples.restaurant.order.model;

import com.wnowakcraft.samples.restaurant.core.domain.DomainBoundBusinessId;

public final class MenuItemId extends DomainBoundBusinessId {
    private MenuItemId(String aggregateId, String domainName, String aggregateName, char aggregateTypeSymbol) {
        super(aggregateId, domainName, aggregateName, aggregateTypeSymbol);
    }

    public static MenuItemId of(String aggregateId)
    {
        return new MenuItemId(aggregateId, "RESTAURANT", "MENU_ITEM", 'A');
    }
}
