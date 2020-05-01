package com.wnowakcraft.samples.restaurant.core.domain.model;

import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
public class TestSnapshot implements Snapshot<TestSnapshot.Id, TestAggregateId> {
    private static final Instant INSTANT_NOW = Instant.now();
    private static final Aggregate.Version DEFAULT_AGGREGATE_VERSION = Aggregate.Version.of(1);
    public static final TestSnapshot DEFAULT = new TestSnapshot(Id.DEFAULT, TestAggregateId.DEFAULT_ONE, INSTANT_NOW, DEFAULT_AGGREGATE_VERSION);

    private final Id snapshotId;
    private final TestAggregateId aggregateId;
    private final Instant creationDate;
    private final Aggregate.Version aggregateVersion;

    public static TestSnapshot ofVersion(Aggregate.Version version) {
        return new TestSnapshot(Id.any(), TestAggregateId.DEFAULT_ONE, INSTANT_NOW, version);
    }

    @Override
    public Id getSnapshotId() {
        return snapshotId;
    }

    @Override
    public TestAggregateId getAggregateId() {
        return aggregateId;
    }

    @Override
    public Instant getCreationDate() {
        return creationDate;
    }

    @Override
    public Aggregate.Version getAggregateVersion() {
        return aggregateVersion;
    }

    public static class Id extends Snapshot.Id {
        private static final String TEST_DOMAIN_NAME = "TEST";
        private static final String TEST_DOMAIN_OBJECT_NAME = "TEST";
        public static final String TEST_SNAPSHOT_ID = TEST_DOMAIN_NAME + "-" + TEST_DOMAIN_OBJECT_NAME + "-S-75a03383-694a-4b78";
        public static final Id DEFAULT = new Id(TEST_SNAPSHOT_ID, TEST_DOMAIN_NAME, TEST_DOMAIN_OBJECT_NAME);

        public static Id any() {
            return new Id(TEST_DOMAIN_NAME, TEST_DOMAIN_OBJECT_NAME);
        }

        private Id(String domainName, String domainObjectName) {
            super(domainName, domainObjectName);
        }

        private Id(String snapshotId, String domainName, String domainObjectName) {
            super(snapshotId, domainName, domainObjectName);
        }
    }
}
