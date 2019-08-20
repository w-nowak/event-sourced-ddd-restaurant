package com.wnowakcraft.samples.restaurant.core.domain.model;

import lombok.*;

import java.time.Instant;

import static lombok.AccessLevel.PROTECTED;

@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor(access = PROTECTED)
public abstract class AbstractSnapshot<ID extends Snapshot.Id, AID extends Aggregate.Id>
        implements Snapshot<ID, AID> {
    @NonNull private final ID snapshotId;
    @NonNull private final AID aggregateId;
    @NonNull private final Instant creationDate;
    @NonNull private final Aggregate.Version aggregateVersion;
}
