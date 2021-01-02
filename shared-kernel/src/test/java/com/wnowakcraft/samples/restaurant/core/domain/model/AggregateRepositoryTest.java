package com.wnowakcraft.samples.restaurant.core.domain.model;

import com.google.common.testing.NullPointerTester;
import com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate.Version;
import com.wnowakcraft.samples.restaurant.core.domain.model.AggregateRepository.RestoreAggregateFromEvents;
import com.wnowakcraft.samples.restaurant.core.domain.model.AggregateRepository.RestoreAggregateFromSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;
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
    private Fixture fixture;

    @BeforeEach
    public void setUp() {
        fixture = new Fixture();
    }

    @AfterEach
    public void verifyInteractions() {
        fixture.thenVerifyAllNecessaryInteractionsWithDependencies();
        fixture.andVerifyNoMoreInteractionsWithTheseDependencies();
    }

    @Test
    void save_storesAggregateChangesIntoEventStore_andReturnsCorrectAggregateVersion() throws Exception {
        fixture.givenAn(AGGREGATE);
        fixture.givenAggregateChangesAreAppendedToEventStore();

        fixture.whenSaveIsCalled();

        fixture.thenReturnedVersionIsNextAggregateVersion();
        fixture.andVerifyNoInteractionWithDependenciesOtherThanEventStore();
    }

    @Test
    void getById_returnsAggregateRecreatedFromEvents_whenNoSnapshotIsAvailable() {
        var restoreEvents = List.<Event>of(AGGREGATE_INIT_EVENT, AGGREGATE_SAMPLE_EVENT);
        fixture.givenAn(AGGREGATE);
        fixture.givenNoSnapshotIsAvailableForGivenAggregate();
        fixture.givenEventStoreReturnsEventStreamWithAll(restoreEvents);
        fixture.givenAggregateIsRestoredWithJustEvents();

        fixture.whenGetByIdIsCalled();

        fixture.thenTheExpectedAggregateShouldBeReturned();
        fixture.andVerifyNoInteractionWithRestoreAggregateFromSnapshotAndEvents();
    }

    @Test
    void getById_returnsAggregateRecreatedFromSnapshotAndEvents_whenSnapshotIsAvailable() {
        var restoreEvents = List.<Event>of(AGGREGATE_SAMPLE_EVENT);
        fixture.givenAn(AGGREGATE);
        fixture.givenSnapshotIsAvailableForGivenAggregate();
        fixture.givenEventStoreForGivenAggregateVersionReturnsEventStreamWith(restoreEvents);
        fixture.givenAggregateIsRestoredWithSnapshotAndEvents();

        fixture.whenGetByIdIsCalled();

        fixture.thenTheExpectedAggregateShouldBeReturned();
        fixture.andVerifyNoInteractionWithRestoreAggregateFromEvents();
    }

    @Test
    void getById_returnsAggregateRecreatedFromSnapshotAndEmptyStreamOfEvents_whenSnapshotIsAvailable_andEmptyEventStreamIsReturned() {
        var restoreEvents = Collections.<Event>emptyList();
        fixture.givenAn(AGGREGATE);
        fixture.givenSnapshotIsAvailableForGivenAggregate();
        fixture.givenEventStoreForGivenAggregateVersionReturnsEventStreamWith(restoreEvents);
        fixture.givenAggregateIsRestoredWithSnapshotAndEvents();

        fixture.whenGetByIdIsCalled();

        fixture.thenTheExpectedAggregateShouldBeReturned();
        fixture.andVerifyNoInteractionWithRestoreAggregateFromEvents();
    }

    @Test
    void verifiesNullPointerContractOfPublicInstanceMethods() {
        fixture.verifiesNullPointerContractOfPublicInstanceMethods();
    }

    @Test
    void verifiesNullPointerContractOfPublicConstructor() {
        fixture.verifiesNullPointerContractOfPublicConstructor();
    }

    private static class Fixture {
        @Mock private EventStore<Event, Aggregate, AggregateId> eventStore;
        @Mock private SnapshotRepository<ModelTestData.Snapshot, ModelTestData.AggregateId> snapshotRepository;
        @Mock private RestoreAggregateFromSnapshot<Event, Aggregate, Snapshot, AggregateId> restoreAggregateFromSnapshot;
        @Mock private RestoreAggregateFromEvents<Event, Aggregate, AggregateId> restoreAggregateFromEvents;
        private AggregateRepository<Event, Aggregate, Snapshot, AggregateId> aggregateRepository;

        private Aggregate aggregate;
        private Aggregate actualAggregate;
        private Snapshot snapshot;
        private Collection<Event> events;
        private Version aggregateVersion;
        private Collection<Runnable> interactions = new ArrayList<>();
        private Collection<Object> interactionObjects = new ArrayList<>();

        public Fixture() {
            MockitoAnnotations.initMocks(this);
            aggregateRepository =
                    new AggregateRepository<>(eventStore, snapshotRepository, restoreAggregateFromSnapshot, restoreAggregateFromEvents);
        }

        public void givenAn(Aggregate aggregate) {
            this.aggregate = aggregate;
        }

        public void givenAggregateChangesAreAppendedToEventStore() {
            given(eventStore.append(aggregate.getId(), aggregate.getVersion(), aggregate.getChanges()))
                    .willReturn(CompletableFuture.completedFuture(AGGREGATE_VERSION_2));
            interactions.add(() -> then(eventStore).should().append(aggregate.getId(), aggregate.getVersion(), aggregate.getChanges()));
        }

        public void whenSaveIsCalled() throws Exception {
            aggregateVersion = aggregateRepository.save(AGGREGATE).get();
        }

        public void thenReturnedVersionIsNextAggregateVersion() {
            assertThat(aggregateVersion).isEqualTo(AGGREGATE_VERSION_2);
        }

        public void givenSnapshotIsAvailableForGivenAggregate() {
            given(snapshotRepository.findLatestSnapshotFor(aggregate.getId())).willReturn(Optional.of(Snapshot.DEFAULT));
            addInteraction(snapshotRepository, () -> then(snapshotRepository).should().findLatestSnapshotFor(aggregate.getId()));
        }

        public void givenNoSnapshotIsAvailableForGivenAggregate() {
            given(snapshotRepository.findLatestSnapshotFor(aggregate.getId())).willReturn(empty());
            addInteraction(snapshotRepository, () -> then(snapshotRepository).should().findLatestSnapshotFor(aggregate.getId()));
        }

        public void givenEventStoreReturnsEventStreamWithAll(List<Event> events) {
            this.events = events;
            given(eventStore.loadAllEventsFor(aggregate.getId())).willReturn(new EventStream(events, AGGREGATE_VERSION_2));
            addInteraction(eventStore, () -> then(eventStore).should().loadAllEventsFor(aggregate.getId()));
        }

        public void givenEventStoreForGivenAggregateVersionReturnsEventStreamWith(List<Event> events) {
            this.events = events;
            given(eventStore.loadEventsFor(aggregate.getId(), AGGREGATE_VERSION_2)).willReturn(new EventStream(events, AGGREGATE_VERSION_2));
            addInteraction(eventStore, () -> then(eventStore).should().loadEventsFor(aggregate.getId(), AGGREGATE_VERSION_2));
        }

        public void givenAggregateIsRestoredWithJustEvents() {
            given(restoreAggregateFromEvents.restore(events, AGGREGATE_VERSION_2)).willReturn(aggregate);
            addInteraction(restoreAggregateFromEvents, () -> then(restoreAggregateFromEvents).should().restore(events, AGGREGATE_VERSION_2));
        }

        public void givenAggregateIsRestoredWithSnapshotAndEvents() {
            given(restoreAggregateFromSnapshot.restore(Snapshot.DEFAULT, events, AGGREGATE_VERSION_2)).willReturn(aggregate);
            addInteraction(restoreAggregateFromSnapshot, () -> then(restoreAggregateFromSnapshot).should().restore(Snapshot.DEFAULT, events, AGGREGATE_VERSION_2));
        }

        public void whenGetByIdIsCalled() {
            actualAggregate = aggregateRepository.getById(aggregate.getId());
        }

        public void thenTheExpectedAggregateShouldBeReturned() {
            assertThat(actualAggregate).isEqualTo(aggregate);
        }

        public void andVerifyNoInteractionWithRestoreAggregateFromSnapshotAndEvents() {
            then(restoreAggregateFromSnapshot).shouldHaveNoInteractions();
        }

        public void andVerifyNoInteractionWithRestoreAggregateFromEvents() {
            then(restoreAggregateFromEvents).shouldHaveNoInteractions();
        }

        public void andVerifyNoInteractionWithDependenciesOtherThanEventStore() {
            then(snapshotRepository).shouldHaveNoInteractions();
            then(restoreAggregateFromEvents).shouldHaveNoInteractions();
            then(restoreAggregateFromSnapshot).shouldHaveNoInteractions();

        }

        public void thenVerifyAllNecessaryInteractionsWithDependencies() {
            interactions.forEach(Runnable::run);
        }

        public void andVerifyNoMoreInteractionsWithTheseDependencies() {
            interactionObjects.forEach(interactionObject -> then(interactionObject).shouldHaveNoMoreInteractions());
        }

        private void addInteraction(Object interactionObject, Runnable interaction) {
            interactionObjects.add(interactionObject);
            interactions.add(interaction);
        }

        public void verifiesNullPointerContractOfPublicInstanceMethods() {
            new NullPointerTester().testAllPublicInstanceMethods(aggregateRepository);
        }

        public void verifiesNullPointerContractOfPublicConstructor() {
            new NullPointerTester().testAllPublicConstructors(AggregateRepository.class);
        }
    }
}