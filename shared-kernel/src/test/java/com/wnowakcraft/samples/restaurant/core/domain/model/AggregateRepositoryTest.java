package com.wnowakcraft.samples.restaurant.core.domain.model;

import com.google.common.testing.NullPointerTester;
import com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate.Version;
import com.wnowakcraft.samples.restaurant.core.domain.model.AggregateRepository.RestoreAggregateFromEvents;
import com.wnowakcraft.samples.restaurant.core.domain.model.AggregateRepository.RestoreAggregateFromSnapshot;
import com.wnowakcraft.samples.restaurant.core.domain.model.snapshot.TakeSnapshotStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.wnowakcraft.samples.restaurant.core.domain.model.ModelTestData.Aggregate;
import static com.wnowakcraft.samples.restaurant.core.domain.model.ModelTestData.Event;
import static com.wnowakcraft.samples.restaurant.core.domain.model.ModelTestData.Snapshot;
import static com.wnowakcraft.samples.restaurant.core.domain.model.ModelTestData.*;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

class AggregateRepositoryTest {
    private Fixture fixture;

    @BeforeEach
    public void setUp() {
        fixture = new Fixture();
        fixture.givenAn(Aggregate.ofVersion(Aggregate.VERSION_1));
    }

    @AfterEach
    public void verifyInteractions() {
        fixture.thenVerifyAllNecessaryInteractionsWithDependencies();
        fixture.andVerifyNoMoreInteractionsWithTheseDependencies();
    }

    @Test
    void save_storesAggregateChangesIntoEventStore_andReturnsCorrectAggregateVersion() throws Exception {
        fixture.givenAggregateChangesAreAppendedToEventStoreAndNewAggregateVersionIs(Aggregate.VERSION_2);
        fixture.givenTakeSnapshotStrategyReturns(false);

        fixture.whenSaveIsCalled();

        fixture.thenReturnedVersionIs(Aggregate.VERSION_2);
        fixture.thenAggregateVersionIsUpdatedTo(Aggregate.VERSION_2);
        fixture.thenThereShouldBeNoInteractionWithSnapshotRepository();
        fixture.andThereShouldBeNoInteractionWithRestoringAggregateDependencies();
    }

    @Test
    void save_storesAggregateChangesIntoEventStore_createsAggregateSnapshot_andReturnsCorrectAggregateVersion() throws Exception {
        fixture.givenAggregateChangesAreAppendedToEventStoreAndNewAggregateVersionIs(Aggregate.VERSION_2);
        fixture.givenTakeSnapshotStrategyReturns(true);

        fixture.whenSaveIsCalled();

        fixture.thenReturnedVersionIs(Aggregate.VERSION_2);
        fixture.thenAggregateVersionIsUpdatedTo(Aggregate.VERSION_2);
        fixture.thenSnapshotOfAggregateShouldBeTaken();
        fixture.andThereShouldBeNoInteractionWithRestoringAggregateDependencies();
    }

    @Test
    void getById_returnsAggregateRecreatedFromEvents_whenNoSnapshotIsAvailable() {
        var restoreEvents = List.<Event>of(Aggregate.INIT_EVENT, Aggregate.SAMPLE_EVENT);
        fixture.givenNoSnapshotIsAvailableForGivenAggregate();
        fixture.givenEventStoreReturnsEventStreamWithAll(restoreEvents);
        fixture.givenAggregateIsRestoredWithJustEvents();

        fixture.whenGetByIdIsCalled();

        fixture.thenTheExpectedAggregateShouldBeReturned();
        fixture.andThereShouldBeNoInteractionWithRestoreAggregateFromSnapshotAndEvents();
    }

    @Test
    void getById_returnsAggregateRecreatedFromSnapshotAndEvents_whenSnapshotIsAvailable() {
        var restoreEvents = List.<Event>of(Aggregate.SAMPLE_EVENT);
        fixture.givenSnapshotIsAvailableForGivenAggregate();
        fixture.givenEventStoreForGivenAggregateVersionReturnsEventStreamWith(restoreEvents);
        fixture.givenAggregateIsRestoredWithSnapshotAndEvents();

        fixture.whenGetByIdIsCalled();

        fixture.thenTheExpectedAggregateShouldBeReturned();
        fixture.andThereShouldBeNoInteractionWithRestoreAggregateFromEvents();
    }

    @Test
    void getById_returnsAggregateRecreatedFromSnapshotAndEmptyStreamOfEvents_whenSnapshotIsAvailable_andEmptyEventStreamIsReturned() {
        var restoreEvents = Collections.<Event>emptyList();
        fixture.givenSnapshotIsAvailableForGivenAggregate();
        fixture.givenEventStoreForGivenAggregateVersionReturnsEventStreamWith(restoreEvents);
        fixture.givenAggregateIsRestoredWithSnapshotAndEvents();

        fixture.whenGetByIdIsCalled();

        fixture.thenTheExpectedAggregateShouldBeReturned();
        fixture.andThereShouldBeNoInteractionWithRestoreAggregateFromEvents();
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
        @Mock private TakeSnapshotStrategy<Event, Aggregate, Snapshot, AggregateId> takeSnapshotStrategy;
        @Mock private RestoreAggregateFromSnapshot<Event, Aggregate, Snapshot, AggregateId> restoreAggregateFromSnapshot;
        @Mock private RestoreAggregateFromEvents<Event, Aggregate, AggregateId> restoreAggregateFromEvents;
        private AggregateRepository<Event, Aggregate, Snapshot, AggregateId> aggregateRepository;

        private Aggregate aggregate;
        private Aggregate actualAggregate;
        private Version actualAggregateVersion;
        private Snapshot snapshot;
        private Collection<Event> events;
        private Collection<Runnable> interactions = new ArrayList<>();
        private Collection<Object> interactionObjects = new ArrayList<>();

        public Fixture() {
            MockitoAnnotations.initMocks(this);
            aggregateRepository =
                    new AggregateRepository<>(
                            eventStore, snapshotRepository, takeSnapshotStrategy, restoreAggregateFromSnapshot, restoreAggregateFromEvents
                    );
        }

        public void givenAn(Aggregate aggregate) {
            this.aggregate = aggregate;
        }

        public void givenAggregateChangesAreAppendedToEventStoreAndNewAggregateVersionIs(Aggregate.Version newVersion) {
            var oldAggregateVersion = aggregate.getVersion();
            given(eventStore.append(aggregate.getId(), oldAggregateVersion, aggregate.getChanges()))
                    .willReturn(CompletableFuture.completedFuture(newVersion));
            interactions.add(() -> then(eventStore).should().append(aggregate.getId(), oldAggregateVersion, aggregate.getChanges()));
        }

        public void whenSaveIsCalled() throws Exception {
            actualAggregateVersion = aggregateRepository.save(aggregate).get();
        }

        public void thenReturnedVersionIs(Aggregate.Version expectedVersion) {
            assertThat(actualAggregateVersion).isEqualTo(expectedVersion);
        }

        public void thenAggregateVersionIsUpdatedTo(Aggregate.Version expectedVersion) {
            assertThat(aggregate.getVersion()).isEqualTo(expectedVersion);
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
            given(eventStore.loadAllEventsFor(aggregate.getId())).willReturn(new EventStream(events, Aggregate.VERSION_2));
            addInteraction(eventStore, () -> then(eventStore).should().loadAllEventsFor(aggregate.getId()));
        }

        public void givenEventStoreForGivenAggregateVersionReturnsEventStreamWith(List<Event> events) {
            this.events = events;
            given(eventStore.loadEventsFor(aggregate.getId(), Aggregate.VERSION_2)).willReturn(new EventStream(events, Aggregate.VERSION_2));
            addInteraction(eventStore, () -> then(eventStore).should().loadEventsFor(aggregate.getId(), Aggregate.VERSION_2));
        }

        public void givenAggregateIsRestoredWithJustEvents() {
            given(restoreAggregateFromEvents.restore(events, Aggregate.VERSION_2)).willReturn(aggregate);
            addInteraction(restoreAggregateFromEvents, () -> then(restoreAggregateFromEvents).should().restore(events, Aggregate.VERSION_2));
        }

        public void givenAggregateIsRestoredWithSnapshotAndEvents() {
            given(restoreAggregateFromSnapshot.restore(Snapshot.DEFAULT, events, Aggregate.VERSION_2)).willReturn(aggregate);
            addInteraction(restoreAggregateFromSnapshot, () -> then(restoreAggregateFromSnapshot).should().restore(Snapshot.DEFAULT, events, Aggregate.VERSION_2));
        }

        public void givenTakeSnapshotStrategyReturns(boolean shouldTakeSnapshot) {
            given(takeSnapshotStrategy.shouldTakeNewSnapshot(eq(aggregate), any(Function.class))).willReturn(shouldTakeSnapshot);
            addInteraction(restoreAggregateFromSnapshot, () -> then(takeSnapshotStrategy).should().shouldTakeNewSnapshot(eq(aggregate), any(Function.class)));
        }

        public void whenGetByIdIsCalled() {
            actualAggregate = aggregateRepository.getById(aggregate.getId());
        }

        public void thenTheExpectedAggregateShouldBeReturned() {
            assertThat(actualAggregate).isEqualTo(aggregate);
        }

        public void thenSnapshotOfAggregateShouldBeTaken() {
            then(snapshotRepository).should().addNewSnapshot(aggregate.takeSnapshot());
        }

        public void thenThereShouldBeNoInteractionWithSnapshotRepository() {
            then(snapshotRepository).shouldHaveNoInteractions();
        }

        public void andThereShouldBeNoInteractionWithRestoreAggregateFromSnapshotAndEvents() {
            then(restoreAggregateFromSnapshot).shouldHaveNoInteractions();
        }

        public void andThereShouldBeNoInteractionWithRestoreAggregateFromEvents() {
            then(restoreAggregateFromEvents).shouldHaveNoInteractions();
        }

        public void andThereShouldBeNoInteractionWithRestoringAggregateDependencies() {
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