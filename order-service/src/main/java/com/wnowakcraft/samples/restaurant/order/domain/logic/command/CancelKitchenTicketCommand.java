package com.wnowakcraft.samples.restaurant.order.domain.logic.command;

import com.wnowakcraft.samples.restaurant.core.domain.model.AbstractCommand;
import com.wnowakcraft.samples.restaurant.order.domain.model.KitchenTicketId;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class CancelKitchenTicketCommand extends AbstractCommand {
    @NonNull private final KitchenTicketId kitchenTicketId;
}
