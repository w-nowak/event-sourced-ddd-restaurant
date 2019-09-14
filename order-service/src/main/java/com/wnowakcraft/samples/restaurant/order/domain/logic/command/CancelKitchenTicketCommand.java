package com.wnowakcraft.samples.restaurant.order.domain.logic.command;

import com.wnowakcraft.samples.restaurant.core.domain.model.Command;
import com.wnowakcraft.samples.restaurant.order.domain.model.KitchenTicketId;
import lombok.NonNull;
import lombok.Value;

@Value
public class CancelKitchenTicketCommand implements Command {
    @NonNull private final KitchenTicketId kitchenTicketId;
}
