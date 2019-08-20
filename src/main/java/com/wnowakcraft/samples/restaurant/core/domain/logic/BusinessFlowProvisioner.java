package com.wnowakcraft.samples.restaurant.core.domain.logic;

import com.wnowakcraft.samples.restaurant.core.domain.model.Event;

public interface BusinessFlowProvisioner<E extends Event> {
    void provision(BusinessFlow<E> businessFlow);
}
