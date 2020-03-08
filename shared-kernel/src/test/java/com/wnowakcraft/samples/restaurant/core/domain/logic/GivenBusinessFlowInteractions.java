package com.wnowakcraft.samples.restaurant.core.domain.logic;

import com.wnowakcraft.samples.restaurant.core.domain.model.Command;
import com.wnowakcraft.samples.restaurant.core.domain.model.Event;
import com.wnowakcraft.samples.restaurant.core.domain.model.Response;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;
import static lombok.AccessLevel.PRIVATE;

@Getter
@RequiredArgsConstructor(access = PRIVATE)
public class GivenBusinessFlowInteractions<E extends Event<?>> {
    private final E flowInitEvent;
    private final Collection<Builder<E>.GivenFollowingCommandType<? extends Command>> givenFollowingCommandTypes;
    private final Class<? extends Response> flowFinalizingResponse;

    public static <E extends Event<?>> Builder<E> givenBusinessFlowInitializedWith(E event) {
        return new Builder<>(event);
    }

    @RequiredArgsConstructor
    public static class Builder<E extends Event<?>> {
        private final E flowInitEvent;
        private final Collection<GivenFollowingCommandType<? extends Command>> givenFollowingCommandTypes = new ArrayList<>();

        public <C extends Command> GivenFollowingCommandType<C> givenFollowingCommandTypeSent(Class<C> commandType) {
            requireNonNull(commandType, "commandType");

            return givenFollowing(commandType);
        }

        private <C extends Command> GivenFollowingCommandType<C> givenFollowing(Class<C> commandType) {
            return new GivenFollowingCommandType<>(commandType);
        }

        private <C extends Command> GivenFollowingCommandType<C> givenNextFollowingCommandType(GivenFollowingCommandType<? extends Command> givenFollowingCommandType, Class<C> nextCommandType) {
            this.givenFollowingCommandTypes.add(givenFollowingCommandType);
            return givenFollowing(nextCommandType);
        }

        private GivenBusinessFlowInteractions<E> thenDone(GivenFollowingCommandType<? extends Command> givenFollowingCommandType,
                                                       Class<? extends Response> thenFlowFinishedOnResponseType) {
            this.givenFollowingCommandTypes.add(givenFollowingCommandType);
            return new GivenBusinessFlowInteractions<>(flowInitEvent, givenFollowingCommandTypes, thenFlowFinishedOnResponseType);
        }

        @Getter
        @RequiredArgsConstructor(access = PRIVATE)
        public class GivenFollowingCommandType<CMD extends Command> {
            private final Class<CMD> command;
            private Collection</*? super */Function<? super CMD, Response>> responses = new ArrayList<>();

            public GivenFollowingCommandType<CMD> thenRespondWith(Function<CMD, Response> responseProvider) {
                requireNonNull(responseProvider, "responseProvider");

                this.responses.add(responseProvider);
                return this;
            }

            public GivenFollowingCommandType<CMD> thenRespondWith(Response response) {
                requireNonNull(response, "response");

                this.responses.add(anyCommand -> response);
                return this;
            }

            public <C extends Command> GivenFollowingCommandType<C> givenFollowingCommandTypeSent(Class<C> commandType) {
                requireNonNull(commandType, "commandType");

                return givenNextFollowingCommandType(this, commandType);
            }

            public WhenResponseTypeReceived whenFollowingResponseIsReceived(Class<? extends Response> responseType) {
                return new WhenResponseTypeReceived(responseType, this);
            }
        }

        @Getter
        @RequiredArgsConstructor(access = PRIVATE)
        public class WhenResponseTypeReceived {
            private final Class<? extends Response> responseType;
            private final GivenFollowingCommandType<? extends Command> lastGivenFollowingCommandType;

            public GivenBusinessFlowInteractions<E> thenFlowIsFinished() {
                return thenDone(lastGivenFollowingCommandType, responseType);
            }
        }
    }

}
