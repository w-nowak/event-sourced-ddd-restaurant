package com.wnowakcraft.samples.restaurant.order.infrastructure.data.store;

import com.google.protobuf.Message;
import com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate;
import com.wnowakcraft.samples.restaurant.core.domain.model.Event;
import com.wnowakcraft.samples.restaurant.core.domain.model.ModelTestData;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.conversion.MessageConverter;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard.KafkaPartition;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard.ShardManager;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard.ShardManager.ShardRef;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard.ShardMetadataProvider;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.wnowakcraft.preconditions.Preconditions.requireThat;
import static java.util.Collections.singleton;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.summingInt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.*;

@DisplayName("Tests KafkaSnapshotRepository with real implementation of RecordSearchStrategy - namely: RecordBinarySearchStrategy")
class KafkaSnapshotRepositoryTest {
    public static final int SNAPSHOT_NOT_FOUND_OFFSET = -1;
    private Fixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new Fixture();
    }

    @DisplayName("Should return latest snapshot for event's sequence number:")
    @ParameterizedTest(name = "given versions of snapshots in a topic {0} and sequence number to look for is {1}, " +
            "then version of snapshot being found is {2} and following offsets were tried {3}")
    @MethodSource({ "latestSnapshotByEventSequenceNumberTestCases" })
    void returnsLatestSnapshotForEventGivenSequenceNumber(List<Integer> versionsOfSnapshotsInTopic, int eventSequenceNumberToLookFor,
                                                          int versionOfSnapshotFound, List<Integer> offsetsTried) {
        fixture.givenShardForBusinessId();
        fixture.givenSnapshotsInTopicWithFollowingVersions(versionsOfSnapshotsInTopic);
        fixture.givenKafkaConsumerIsProvided();
        fixture.givenRealBinarySearchStrategyIsUsed();
        fixture.givenSingleRecordIsReadForOffsetSelectedBySearchStrategy();
        fixture.whenFindLatestSnapshotMethodIsCalledFor(eventSequenceNumberToLookFor);
        fixture.thenSnapshotWithTheFollowingVersionIsSelected(versionOfSnapshotFound);
        fixture.thenTheFollowingOffsetsWereTried(offsetsTried);
    }

    @Test
    void returnsEmptyOptionalWhenThereAreNoSnapshotsInTheTopic() {
        var anySequenceNumber = 10;

        fixture.givenShardForBusinessId();
        fixture.givenNoSnapshotsInTopic();
        fixture.whenFindLatestSnapshotMethodIsCalledFor(anySequenceNumber);
        fixture.thenSearchStrategyFactoryIsNeverUsed();
        fixture.thenNoSnapshotIsRead();
        fixture.thenNoSnapshotIsReturned();
        fixture.thenNoOffsetsWereTried();
    }

    static Stream<? extends Arguments> latestSnapshotByEventSequenceNumberTestCases() {
        return Stream.of(
          arguments(givenSnapshotsWithVersions(5, 20, 25, 32, 40, 60, 73, 75, 88, 95, 100), whenWeLookForSnapshotWithEventSequenceOf(85),
                  thenSnapshotWithFollowingVersionIsSelected(75), andOffsetsWhichWereTriedAre(10, 5, 8, 6, 7)),
                arguments(givenSnapshotsWithVersions(5, 20, 25, 32, 40, 60, 73, 75, 88, 95, 100), whenWeLookForSnapshotWithEventSequenceOf(74),
                        thenSnapshotWithFollowingVersionIsSelected(73), andOffsetsWhichWereTriedAre(10, 5, 8, 6, 7, 6)),
                arguments(givenSnapshotsWithVersions(5, 20, 25, 32, 40, 60, 73, 75, 88, 95, 100), whenWeLookForSnapshotWithEventSequenceOf(61),
                        thenSnapshotWithFollowingVersionIsSelected(60), andOffsetsWhichWereTriedAre(10, 5, 8, 6, 5)),
                arguments(givenSnapshotsWithVersions(5, 20, 25, 32, 40, 60, 73, 75, 88, 95, 100), whenWeLookForSnapshotWithEventSequenceOf(41),
                        thenSnapshotWithFollowingVersionIsSelected(40), andOffsetsWhichWereTriedAre(10, 5, 2, 4)),
                arguments(givenSnapshotsWithVersions(5, 20, 25, 32, 40, 60, 73, 75, 88, 95, 100), whenWeLookForSnapshotWithEventSequenceOf(100),
                        thenSnapshotWithFollowingVersionIsSelected(95), andOffsetsWhichWereTriedAre(10, 5, 8, 9)),
                arguments(givenSnapshotsWithVersions(5, 20, 25, 32, 40, 60, 73, 75, 88, 95, 100), whenWeLookForSnapshotWithEventSequenceOf(150),
                        thenSnapshotWithFollowingVersionIsSelected(100), andOffsetsWhichWereTriedAre(10)),
                arguments(givenSnapshotsWithVersions(5, 20, 25, 32, 40, 60, 73, 75, 88, 95, 100), whenWeLookForSnapshotWithEventSequenceOf(10),
                        thenSnapshotWithFollowingVersionIsSelected(5), andOffsetsWhichWereTriedAre(5, 2, 1, 0)),
                arguments(givenSnapshotsWithVersions(5, 20, 25, 32, 40, 60, 73, 75, 88, 95, 100), whenWeLookForSnapshotWithEventSequenceOf(2),
                        thenNoSnapshotWasFound(), andOffsetsWhichWereTriedAre(5, 2, 1, 0)),
                arguments(givenSnapshotsWithVersions(5, 20), whenWeLookForSnapshotWithEventSequenceOf(7),
                        thenSnapshotWithFollowingVersionIsSelected(5), andOffsetsWhichWereTriedAre(1, 0)),
                arguments(givenSnapshotsWithVersions(5), whenWeLookForSnapshotWithEventSequenceOf(7),
                        thenSnapshotWithFollowingVersionIsSelected(5), andOffsetsWhichWereTriedAre(0)),
                arguments(givenSnapshotsWithVersions(5), whenWeLookForSnapshotWithEventSequenceOf(3),
                        thenNoSnapshotWasFound(), andOffsetsWhichWereTriedAre(0))
        );
    }

    private static List<Integer> givenSnapshotsWithVersions(Integer... snapshotVersions) {
        requireThat(snapshotVersions.length > 0, "At least one snapshot version needs to be specified");
        return List.of(snapshotVersions);
    }

    private static int whenWeLookForSnapshotWithEventSequenceOf(int eventSequenceNumber) {
        return eventSequenceNumber;
    }

    private static int thenSnapshotWithFollowingVersionIsSelected(int snapshotVersion) {
        requireThat(snapshotVersion > -1, "snapshotVersion cannot be negative");
        return snapshotVersion;
    }

    private static int thenNoSnapshotWasFound() {
        return SNAPSHOT_NOT_FOUND_OFFSET;
    }

    private static List<Integer> andOffsetsWhichWereTriedAre(Integer... offsets) {
        requireThat(offsets.length > 0, "At least one offset needs to be specified");
        return List.of(offsets);
    }

    private static class Fixture {
        private static final ModelTestData.AggregateId AGGREGATE_ID = ModelTestData.AggregateId.DEFAULT_ONE;
        private static final int SHARD_NUMBER = 0;
        private static final ShardRef SHARD_REF = new ShardRef("test_topic", SHARD_NUMBER);
        public static final TopicPartition PARTITION_ASSIGNMENT = KafkaPartition.of(SHARD_REF);
        private static final int ONE_RECORD = 1;
        private static List<ModelTestData.Snapshot> snapshotsInTopic;

        @Mock private ShardManager shardManager;
        @Mock private ShardMetadataProvider shardMetadataProvider;
        @Mock private Consumer<String, Message> recordConsumer;
        @Mock private KafkaProducerFactory producerFactory;
        @Mock private KafkaConsumerFactory consumerFactory;
        @Mock private KafkaRecordReader<ModelTestData.Snapshot> recordReader;
        @Mock private RecordSearchStrategyFactory recordSearchStrategyFactory;
        @Mock private MessageConverter<ModelTestData.Snapshot, Message> snapshotMessageConverter;
        private KafkaSnapshotRepository<ModelTestData.Snapshot, ModelTestData.AggregateId> snapshotRepository;
        private ModelTestData.Snapshot foundSnapshot;

        Fixture() {
            snapshotsInTopic = new ArrayList<>();
            MockitoAnnotations.initMocks(this);
            snapshotRepository = new KafkaSnapshotRepository<>(consumerFactory, producerFactory, shardManager,
                    shardMetadataProvider, recordReader, recordSearchStrategyFactory, snapshotMessageConverter);
        }

        void givenShardForBusinessId() {
            given(shardManager.getShardForBusinessIdOf(AGGREGATE_ID)).willReturn(SHARD_REF);
        }

        void givenSnapshotsInTopicWithFollowingVersions(List<Integer> snapshotVersions) {
            snapshotVersions.forEach(snapshotVersion -> snapshotsInTopic.add(ModelTestData.Snapshot.ofVersion(Aggregate.Version.of(snapshotVersion))));

            if (!snapshotVersions.isEmpty()) {
                givenLastSnapshotRecordOffsetIs(Math.max(snapshotVersions.size() - 1, 0));
            }
        }

        void givenNoSnapshotsInTopic() {
            snapshotsInTopic.clear();
            givenLastSnapshotRecordOffsetIs(SNAPSHOT_NOT_FOUND_OFFSET);
        }

        private void givenLastSnapshotRecordOffsetIs(long lastRecordOffset) {
            given(shardMetadataProvider.getLastRecordOffsetForShard(SHARD_REF)).willReturn(CompletableFuture.completedFuture(lastRecordOffset));
        }

        void givenKafkaConsumerIsProvided() {
            given(recordConsumer.assignment()).willReturn(Set.of(PARTITION_ASSIGNMENT));
            given(consumerFactory.<Message>createConsumerFor(SHARD_REF)).willReturn(recordConsumer);
        }

        void givenRealBinarySearchStrategyIsUsed() {
            given(recordSearchStrategyFactory.getSearchStrategyFor(anyLong(), anyLong()))
                    .willAnswer(args ->
                            new RecordBinarySearchStrategyFactory()
                                    .getSearchStrategyFor(args.getArgument(0, Long.class), args.getArgument(1, Long.class))
                    );
        }

        void givenSingleRecordIsReadForOffsetSelectedBySearchStrategy() {
            ArgumentCaptor<Long> offsetArgumentCaptor = ArgumentCaptor.forClass(Long.class);
            willDoNothing().given(recordConsumer).seek(eq(PARTITION_ASSIGNMENT), offsetArgumentCaptor.capture());

            given(recordReader.readLimitedNumberOfRecordsFrom(recordConsumer, AGGREGATE_ID, ONE_RECORD))
                    .willAnswer(arguments -> singleton(snapshotsInTopic.get(offsetArgumentCaptor.getValue().intValue())));
        }

        void whenFindLatestSnapshotMethodIsCalledFor(int givenEventSequence) {
            foundSnapshot = snapshotRepository
                    .findLatestSnapshotFor(AGGREGATE_ID, Event.SequenceNumber.of(givenEventSequence))
                    .orElse(null);
        }

        void thenSnapshotWithTheFollowingVersionIsSelected(int expectedFoundSnapshotVersion) {
            if(expectedFoundSnapshotVersion == SNAPSHOT_NOT_FOUND_OFFSET) {
                assertThat(foundSnapshot).isNull();
                return;
            }

            assertThat(foundSnapshot).isNotNull();
            assertThat(foundSnapshot.getAggregateVersion().number).isEqualTo(expectedFoundSnapshotVersion);
        }

        void thenNoSnapshotIsReturned() {
            assertThat(foundSnapshot).isNull();
        }

        void thenNoSnapshotIsRead() {
            then(recordReader).shouldHaveNoInteractions();
        }

        void thenTheFollowingOffsetsWereTried(List<Integer> expectedOffsetBeingTried) {
            expectedOffsetBeingTried.stream()
                    .collect(Collectors.groupingBy(identity(), summingInt(occurrence -> 1)))
                    .forEach((expectedOffset, occurredCount) -> then(recordConsumer).should(times(occurredCount)).seek(PARTITION_ASSIGNMENT, expectedOffset));
        }

        void thenNoOffsetsWereTried() {
            then(recordConsumer).should(never()).seek(any(TopicPartition.class), anyLong());
        }

        public void thenSearchStrategyFactoryIsNeverUsed() {
            then(recordSearchStrategyFactory).shouldHaveNoInteractions();
        }
    }
}