package com.wnowakcraft.samples.restaurant.order.domain.logic.command.response;

import com.wnowakcraft.samples.restaurant.core.domain.model.Response;
import com.wnowakcraft.samples.restaurant.order.domain.model.KitchenTicketId;

public interface KitchenTicketCreatedResponse extends Response {
    KitchenTicketId getKitchenTicketId();
}
