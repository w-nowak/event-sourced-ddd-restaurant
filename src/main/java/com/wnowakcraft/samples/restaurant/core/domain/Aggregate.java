package com.wnowakcraft.samples.restaurant.core.domain;

import com.wnowakcraft.preconditions.Preconditions;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collection;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;
import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;

public interface Aggregate<ID extends Aggregate.Id, E extends Event> {
    ID getId();
    Collection<E> getChanges();
    Version getVersion();

    abstract class Id extends DomainBoundBusinessId {
        private static final char TYPE_SYMBOL = 'A';


        protected Id(String domainName, String domainObjectName) {
            super(domainName, domainObjectName, TYPE_SYMBOL);
        }

        protected Id(String aggregateId, String domainName, String domainObjectName) {
            super(aggregateId, domainName, domainObjectName, TYPE_SYMBOL);
        }
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    @AllArgsConstructor(access = PRIVATE)
    final class Version {
        public final static Version NONE = new Version(0);

        private final long number;

        public static Version of(long number) {
            Preconditions.requireThat(number > 0, "The version number needs to be positive integer");

            return new Version(number);
        }
    }

    interface State {
        String name();
    }

    @Getter
    class IllegalStateChangeException extends RuntimeException {
        private final State fromState;
        private final State toState;
        private final String message;

        public IllegalStateChangeException(Aggregate aggregate, State fromState, State toState, String description) {
            requireNonNull(aggregate, "aggregate");
            this.fromState = requireNonNull(fromState, "fromState");
            this.toState = requireNonNull(toState, "toState");
            requireNonNull(description, "description");
            this.message = format("It's not allowed to change state from %s to %s for %s. %s",
                    fromState.name(), toState.name(), aggregate.getClass().getSimpleName(), description);
        }
    }
}