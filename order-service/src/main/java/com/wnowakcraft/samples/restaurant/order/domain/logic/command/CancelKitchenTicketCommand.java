package com.wnowakcraft.samples.restaurant.order.domain.logic.command;

import com.wnowakcraft.samples.restaurant.core.domain.model.AbstractCommand;
import com.wnowakcraft.samples.restaurant.order.domain.model.KitchenTicketId;
import lombok.NonNull;
import lombok.Value;

@Value
public class CancelKitchenTicketCommand extends AbstractCommand {
    @NonNull private final KitchenTicketId kitchenTicketId;
}
