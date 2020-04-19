package com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard;

import com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public interface ShardManager {
    ShardRef getShardForBusinessIdOf(Aggregate.Id aggregateId);

    @RequiredArgsConstructor
    class ShardRef {
        @NonNull public final String topicName;
        public final int shardId;
    }

    @Qualifier
    @Retention(RUNTIME)
    @Target({ TYPE, PARAMETER, FIELD })
    @interface ShardingStrategy {
        ShardingType value();

        enum ShardingType {
            ANY,
            EVENT_STORE,
            SNAPSHOT_STORE,
            EVENT_PUBLISHING
        }
    }
}
