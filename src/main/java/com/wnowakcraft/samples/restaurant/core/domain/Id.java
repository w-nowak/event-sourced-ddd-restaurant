package com.wnowakcraft.samples.restaurant.core.domain;

import java.io.Serializable;

public interface Id<T extends Serializable & Comparable<T>> {
    T getId();
}
