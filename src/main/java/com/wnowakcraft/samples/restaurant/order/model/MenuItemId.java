package com.wnowakcraft.samples.restaurant.order.model;

import com.wnowakcraft.samples.restaurant.core.domain.AbstractAggregate;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class MenuItemId extends AbstractAggregate.PrefixedUuidAggregateId {
    private MenuItemId(String domainName, String aggregateType, String aggregateId) {
        super(domainName, aggregateType, aggregateId);
    }

    public static MenuItemId of(String aggregateId)
    {
        return new MenuItemId("RESTAURANT", "MENU_ITEM", aggregateId);
    }
}
