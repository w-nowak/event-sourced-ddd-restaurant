package com.wnowakcraft.samples.restaurant.order.domain.logic.command.response;

import com.wnowakcraft.samples.restaurant.core.domain.model.Response;
import com.wnowakcraft.samples.restaurant.order.domain.model.KitchenTicketId;
import lombok.NonNull;
import lombok.Value;

@Value
public class KitchenTicketCreatedResponse implements Response {
    @NonNull private final KitchenTicketId kitchenTicketId;
}
