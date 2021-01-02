package com.wnowakcraft.samples.restaurant.core.domain.model;

import com.wnowakcraft.logging.LogAfter;
import com.wnowakcraft.logging.LogBefore;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static com.wnowakcraft.logging.Level.DEBUG;
import static java.util.Objects.requireNonNull;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = { @Inject })
public class AggregateRepository<
        E extends Event<?>,
        A extends Aggregate<ID, E> & WithUpdatableVersion,
        S extends Snapshot<? extends Snapshot.Id, ID>,
        ID extends Aggregate.Id>  {

    @NonNull private final EventStore<E, A, ID> eventStore;
    @NonNull private final SnapshotRepository<S, ID> snapshotRepository;
    @NonNull private final RestoreAggregateFromSnapshot<E, A, S, ID> restoreAggregateFromSnapshot;
    @NonNull private final RestoreAggregateFromEvents<E, A, ID> restoreAggregateFromEvents;

    @LogBefore(value = "Saving aggregate with id of {p0.getId().getValue()}...", level = DEBUG)
    @LogAfter(value = "Aggregate has been saved.", level = DEBUG)
    public CompletableFuture<Aggregate.Version> save(A aggregate) {
        requireNonNull(aggregate, "aggregate");

        return eventStore.append(aggregate.getId(), aggregate.getVersion(), aggregate.getChanges());
    }

    @LogBefore(value = "Restoring aggregate with id of {p0.getValue()}...", level = DEBUG)
    @LogAfter(value = "Aggregate has been restored.", level = DEBUG)
    public A getById(ID aggregateId) {
        requireNonNull(aggregateId, "aggregateId");

        return snapshotRepository.findLatestSnapshotFor(aggregateId)
                .map(s -> restoreOrderFrom(s, eventStore.loadEventsFor(aggregateId, s.getAggregateVersion().nextVersion())))
                .orElseGet(() -> restoreOrderFrom(eventStore.loadAllEventsFor(aggregateId)));
    }

    private A restoreOrderFrom(S snapshot, EventStore.EventStream<E> eventStream) {
        return restoreAggregateFromSnapshot.restore(
                snapshot,
                eventStream.getEvents(),
                eventStream.isEmpty() ? snapshot.getAggregateVersion() : eventStream.getVersion()
        );
    }

    private  A restoreOrderFrom(EventStore.EventStream<E> eventStream) {
        return restoreAggregateFromEvents.restore(eventStream.getEvents(), eventStream.getVersion());
    }

    public interface RestoreAggregateFromSnapshot <
            E extends Event<?>,
            A extends Aggregate<ID, E> & WithUpdatableVersion,
            S extends Snapshot<? extends Snapshot.Id, ID>,
            ID extends Aggregate.Id> {

        A restore(S snapshot, Collection<E> events, Aggregate.Version version);
    }

    public interface RestoreAggregateFromEvents <
            E extends Event<?>,
            A extends Aggregate<ID, E> & WithUpdatableVersion,
            ID extends Aggregate.Id> {

        A restore(Collection<E> events, Aggregate.Version version);
    }
}
