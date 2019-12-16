package com.wnowakcraft.samples.restaurant.core.domain.model;

public class TestAggregateId extends Aggregate.Id {
    public static final String DOMAIN_NAME = "shared-kernel";
    public static final String AGGREGATE_NAME = "testAggregate";
    public static final TestAggregateId DEFAULT_ONE = new TestAggregateId(DOMAIN_NAME, AGGREGATE_NAME);


    private TestAggregateId(String domainName, String domainObjectName) {
        super(domainName, domainObjectName);
    }

//    private TestAggregateId(String aggregateId, String domainName, String domainObjectName) {
//        super(aggregateId, domainName, domainObjectName);
//    }

}
