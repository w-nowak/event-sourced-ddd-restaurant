package com.wnowakcraft.samples.restaurant.core.domain.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@Getter
@RequiredArgsConstructor(access = PROTECTED)
public class AbstractResponse implements Response {
    @NonNull private final UUID correlationId;
}
