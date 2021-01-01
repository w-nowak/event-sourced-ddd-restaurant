package com.wnowakcraft.samples.restaurant.core.domain.model;

import com.wnowakcraft.samples.restaurant.core.domain.model.AggregateRepository.RestoreAggregateFromEvents;
import com.wnowakcraft.samples.restaurant.core.domain.model.AggregateRepository.RestoreAggregateFromSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.wnowakcraft.samples.restaurant.core.domain.model.ModelTestData.Aggregate;
import static com.wnowakcraft.samples.restaurant.core.domain.model.ModelTestData.Event;
import static com.wnowakcraft.samples.restaurant.core.domain.model.ModelTestData.Snapshot;
import static com.wnowakcraft.samples.restaurant.core.domain.model.ModelTestData.*;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

class AggregateRepositoryTest {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EventStore<Event, Aggregate, AggregateId> eventStore;
    @Mock private SnapshotRepository<ModelTestData.Snapshot, ModelTestData.AggregateId> snapshotRepository;
    @Mock private RestoreAggregateFromSnapshot<Event, Aggregate, Snapshot, AggregateId> restoreAggregateFromSnapshot;
    @Mock private RestoreAggregateFromEvents<Event, Aggregate, AggregateId> restoreAggregateFromEvents;
    private AggregateRepository<Event, Aggregate, Snapshot, AggregateId> aggregateRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        aggregateRepository =
                new AggregateRepository<>(eventStore, snapshotRepository, restoreAggregateFromSnapshot, restoreAggregateFromEvents);
    }

    @Test
    void save_storesAggregateChangesIntoEventStore_andReturnsCorrectAggregateVersion() throws Exception {
        given(eventStore.append(AGGREGATE.getId(), AGGREGATE.getVersion(), AGGREGATE.getChanges()))
                .willReturn(CompletableFuture.completedFuture(AGGREGATE_VERSION_2));

        var futureVersion = aggregateRepository.save(AGGREGATE);

        assertThat(futureVersion.get()).isEqualTo(AGGREGATE_VERSION_2);
    }

    @Test
    void getById_returnsAggregateRecreatedFromEvents_whenNoSnapshotIsAvailable() {
        var restoreEvents = List.<Event>of(AGGREGATE_INIT_EVENT, AGGREGATE_SAMPLE_EVENT);
        given(snapshotRepository.findLatestSnapshotFor(AGGREGATE.getId())).willReturn(empty());
        given(eventStore.loadAllEventsFor(AGGREGATE.getId())).willReturn(new EventStream(restoreEvents, AGGREGATE_VERSION_2));
        given(restoreAggregateFromEvents.restore(restoreEvents, AGGREGATE_VERSION_2)).willReturn(AGGREGATE);

        var actualAggregate = aggregateRepository.getById(AGGREGATE.getId());

        assertThat(actualAggregate).isEqualTo(AGGREGATE);
        then(snapshotRepository).should().findLatestSnapshotFor(AGGREGATE.getId());
        then(eventStore).should().loadAllEventsFor(AGGREGATE.getId());
        then(restoreAggregateFromEvents).should().restore(restoreEvents, AGGREGATE_VERSION_2);

        then(snapshotRepository).shouldHaveNoMoreInteractions();
        then(eventStore).shouldHaveNoMoreInteractions();
        then(restoreAggregateFromEvents).shouldHaveNoMoreInteractions();
        then(restoreAggregateFromSnapshot).shouldHaveNoInteractions();
    }

    @Test
    void getById_returnsAggregateRecreatedFromSnapshotAndEvents_whenSnapshotIsAvailable() {
        var restoreEvents = List.<Event>of(AGGREGATE_INIT_EVENT, AGGREGATE_SAMPLE_EVENT);
        given(snapshotRepository.findLatestSnapshotFor(AGGREGATE.getId())).willReturn(Optional.of(Snapshot.DEFAULT));
        given(eventStore.loadEventsFor(AGGREGATE.getId(), AGGREGATE_VERSION_2)).willReturn(new EventStream(restoreEvents, AGGREGATE_VERSION_2));
        given(restoreAggregateFromSnapshot.restore(Snapshot.DEFAULT, restoreEvents, AGGREGATE_VERSION_2)).willReturn(AGGREGATE);

        var actualAggregate = aggregateRepository.getById(AGGREGATE.getId());

        assertThat(actualAggregate).isEqualTo(AGGREGATE);
        then(snapshotRepository).should().findLatestSnapshotFor(AGGREGATE.getId());
        then(eventStore).should().loadEventsFor(AGGREGATE.getId(), AGGREGATE_VERSION_2);
        then(restoreAggregateFromSnapshot).should().restore(Snapshot.DEFAULT, restoreEvents, AGGREGATE_VERSION_2);

        then(snapshotRepository).shouldHaveNoMoreInteractions();
        then(eventStore).shouldHaveNoMoreInteractions();
        then(restoreAggregateFromSnapshot).shouldHaveNoMoreInteractions();
        then(restoreAggregateFromEvents).shouldHaveNoInteractions();
    }
}