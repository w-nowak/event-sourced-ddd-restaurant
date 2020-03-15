package com.wnowakcraft.samples.restaurant.order.domain.logic.query;

import com.wnowakcraft.samples.restaurant.core.domain.model.AbstractQuery;
import com.wnowakcraft.samples.restaurant.order.domain.model.MenuItemId;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class GetMenuItemQuery extends AbstractQuery {
    @NonNull public final MenuItemId menuItemId;
}
