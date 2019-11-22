package com.wnowakcraft.samples.restaurant.core.domain.model;

import java.io.Serializable;

public interface Id<T extends Serializable & Comparable<T>> {
    T getValue();
}
