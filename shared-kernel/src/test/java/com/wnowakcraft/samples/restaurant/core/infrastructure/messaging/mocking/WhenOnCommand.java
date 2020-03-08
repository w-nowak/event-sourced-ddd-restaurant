package com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.mocking;

import com.google.common.collect.ImmutableList;
import com.wnowakcraft.samples.restaurant.core.domain.model.Command;
import com.wnowakcraft.samples.restaurant.core.domain.model.Response;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;

import static java.util.Collections.singletonList;
import static lombok.AccessLevel.PRIVATE;

public interface WhenOnCommand {

    WhenOnCommand when(Class<? extends Command> commandTypeIssued, ThenRespondWith thenRespondWith);

    @Getter
    @RequiredArgsConstructor(access = PRIVATE)
    class ThenRespondWith {
        private final Collection<Response> commandResponses;

        public static ThenRespondWith thenRespondWith(Response commandResponse) {
            return new ThenRespondWith(singletonList(commandResponse));
        }

        public static ThenRespondWith thenRespondInSequenceWith(Response commandResponse1, Response... commandOtherResponses) {
            var allResponses = ImmutableList.<Response>builder().add(commandResponse1).add(commandOtherResponses).build();
            return new ThenRespondWith(allResponses);
        }
    }
}
