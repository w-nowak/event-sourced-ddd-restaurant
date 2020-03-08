package com.wnowakcraft.samples.restaurant.core.domain.logic;

import com.wnowakcraft.samples.restaurant.core.domain.logic.DefaultBusinessFlowProvisioner.BusinessFlowProvisionerConfig;
import com.wnowakcraft.samples.restaurant.core.domain.logic.GivenBusinessFlowInteractions.Builder;
import com.wnowakcraft.samples.restaurant.core.domain.model.Command;
import com.wnowakcraft.samples.restaurant.core.domain.model.Event;
import com.wnowakcraft.samples.restaurant.core.domain.model.Response;
import com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.mocking.BusinessFlowMock;
import com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.mocking.WhenOnCommand;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

import static com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.mocking.BusinessFlowMock.allowedFlowFinishedResponses;
import static com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.mocking.WhenOnCommand.ThenRespondWith.thenRespondWith;

public class BusinessFlowTestProvisioner<E extends Event<?>, S> implements BusinessFlowProvisioner<E, S> {
    private final GivenBusinessFlowInteractions<E> givenBusinessFlowInteractions;
    private GivenBusinessFlowInteractionsStateHandler<E> givenBusinessFlowInteractionsStateHandler;
    private BusinessFlowMock<E, S> businessFlowMock;

    public BusinessFlowTestProvisioner(GivenBusinessFlowInteractions<E> givenBusinessFlowInteractions,
                                       BusinessFlowProvisionerConfig<E> flowProvisionerConfig){
        this.givenBusinessFlowInteractions = givenBusinessFlowInteractions;
        this.businessFlowMock = new BusinessFlowMock<>(
                flowProvisionerConfig,
                allowedFlowFinishedResponses(givenBusinessFlowInteractions.getFlowFinalizingResponse())
        );
    }

    @Override
    public void provision(BusinessFlowDefinition<E, S> businessFlowDefinition) {
        var businessFlowProvisioner = businessFlowMock.initializeTestProvisioner(DefaultBusinessFlowProvisioner::new);

        givenBusinessFlowInteractionsStateHandler = GivenBusinessFlowInteractionsStateHandler
                .initialize(givenBusinessFlowInteractions.getGivenFollowingCommandTypes(), businessFlowMock.getWhenOnCommandMock());

        businessFlowMock.attachBeforeCommandSentHandler(command -> givenBusinessFlowInteractionsStateHandler.mockResponseFor(command));

        businessFlowProvisioner.provision(businessFlowDefinition);
    }

    public void triggerBusinessFlowInitEvent()
    {
        businessFlowMock.triggerBusinessFlowInitEvent(givenBusinessFlowInteractions.getFlowInitEvent());
    }

    @RequiredArgsConstructor
    private static class GivenBusinessFlowInteractionsStateHandler<E extends Event<?>> {
        @NonNull private final Iterator<Builder<E>.GivenFollowingCommandType<? extends Command>> givenFollowingCommandTypeIterator;
        @NonNull private final WhenOnCommand whenOnCommand;
        private Class<? extends Command> lastMockedCommandType;
        private Iterator<Function<? super Command, Response>> lastMockedCommandResponseMappingIterator;

        static <E extends Event<?>> GivenBusinessFlowInteractionsStateHandler<E> initialize(Collection<Builder<E>.GivenFollowingCommandType<? extends Command>> givenFollowingCommandTypes,
                                                                                            WhenOnCommand whenOnCommand) {
            return new GivenBusinessFlowInteractionsStateHandler<>(givenFollowingCommandTypes.iterator(), whenOnCommand);
        }

        void mockResponseFor(Command command) {
            Function<? super Command, Response> currentCommandResponseMapping = null;

            if(command.getClass() == lastMockedCommandType && lastMockedCommandResponseMappingIterator.hasNext()){
                currentCommandResponseMapping = lastMockedCommandResponseMappingIterator.next();
            }

            if(currentCommandResponseMapping == null && !givenFollowingCommandTypeIterator.hasNext()) {
                return;
            }

            Class<? extends Command> currentCommandType = null;
            Iterator<Function<? super Command, Response>> currentCommandResponseMappingIterator = null;
            if(currentCommandResponseMapping == null) {
                Builder<E>.GivenFollowingCommandType<? extends Command> givenFollowingCommandType = givenFollowingCommandTypeIterator.next();
                 var nextCommandType = givenFollowingCommandType.getCommand();

                if(command.getClass() != nextCommandType) {
                    return;
                }

                currentCommandType = nextCommandType;
                currentCommandResponseMappingIterator = (Iterator<Function<? super Command, Response>>) (Iterator) givenFollowingCommandType.getResponses().iterator();
                currentCommandResponseMapping = currentCommandResponseMappingIterator.next();
            }

            whenOnCommand.when(currentCommandType, thenRespondWith(currentCommandResponseMapping.apply(command)));
            lastMockedCommandType = currentCommandType;
            lastMockedCommandResponseMappingIterator = currentCommandResponseMappingIterator;
        }
    }
}
