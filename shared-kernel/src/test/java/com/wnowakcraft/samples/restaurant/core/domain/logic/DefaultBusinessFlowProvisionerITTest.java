package com.wnowakcraft.samples.restaurant.core.domain.logic;

import com.wnowakcraft.samples.restaurant.core.domain.logic.DefaultBusinessFlowProvisioner.BusinessFlowProvisionerConfig;
import com.wnowakcraft.samples.restaurant.core.domain.model.*;
import com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.*;
import com.wnowakcraft.samples.restaurant.core.infrastructure.saga.BusinessFlowRunner.StateEnvelope;
import com.wnowakcraft.samples.restaurant.core.infrastructure.saga.BusinessFlowStateHandler;
import lombok.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;
import java.util.function.Consumer;

import static com.wnowakcraft.samples.restaurant.core.domain.logic.BusinessFlowDefinition.OnResponse.failureWithCompensation;
import static com.wnowakcraft.samples.restaurant.core.domain.logic.BusinessFlowDefinition.OnResponse.failureWithRetry;
import static com.wnowakcraft.samples.restaurant.core.domain.logic.BusinessFlowDefinition.OnResponse.success;
import static com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.CommandResponseChannelMock.ThenRespondWith.thenRespondWith;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
class DefaultBusinessFlowProvisionerITTest {
    private static final FirstCommand FIRST_COMMAND = new FirstCommand();
    private static final SecondCommand SECOND_COMMAND = new SecondCommand();
    private static final FinishingCommand FINISHING_COMMAND = new FinishingCommand();
    private static final FirstCommandSuccessfulResponse FIRST_COMMAND_SUCCESSFUL_RESPONSE =
            new FirstCommandSuccessfulResponse(UUID.randomUUID());
    private static final SecondCommandSuccessfulResponse SECOND_COMMAND_SUCCESSFUL_RESPONSE =
            new SecondCommandSuccessfulResponse(UUID.randomUUID());
    private static final FinishingCommandSuccessfulResponse FINISHING_COMMAND_SUCCESSFUL_RESPONSE =
            new FinishingCommandSuccessfulResponse(UUID.randomUUID());
    private static final int STATE_INDEX_AFTER_INIT_EVENT_HANDLED = 0;
    private static final int STATE_INDEX_AFTER_FIRST_COMMAND_HANDLED = 1;

    private Fixture fixture;

    @BeforeEach
    void setUp() {
        var initFlowState = new TestState();

        var businessFlowDefinition =
                BusinessFlowDefinition
                        .startWith(TestInitEvent.class, e -> initFlowState)
                            .compensateBy(s -> new InitEventCompensationCommand())
                        .thenSend(s -> FIRST_COMMAND)
                            .on(FirstCommandSuccessfulResponse.class, (resp, state) -> state.firstCommandHandled())
                            .on(FirstCommandErrorResponse.class, failureWithCompensation())
                            .compensateBy(s -> new FirstCommandCompensation())
                        .thenSend(s -> SECOND_COMMAND)
                            .on(SecondCommandSuccessfulResponse.class, (resp, state) -> state.secondCommandHandled())
                            .on(SecondCommandErrorResponse.class, failureWithCompensation())
                            .compensateBy(s -> new FirstCommandCompensation())
                        .thenSend(s -> FINISHING_COMMAND)
                            .on(FinishingCommandSuccessfulResponse.class, success())
                            .on(FinishingCommandErrorResponse.class, failureWithRetry())
                        .done();

        fixture = new Fixture(businessFlowDefinition, initFlowState);
    }

    @Test
    @Timeout(3)
    void successfulFlowPass_forNewBusinessFlow_whenAllSuccessfulResponsesReturned() {
        fixture.givenFlowIsInitialized();
        fixture.givenFlowIsProvisioned();
        fixture.givenFirstCommandReturnsSuccessfulResponse();
        fixture.givenSecondCommandReturnsSuccessfulResponse();
        fixture.givenFinalizingCommandReturnsSuccessfulResponse();
        fixture.whenFlowIsInitiatedByInitEvent();
        fixture.thenWaitUntilFlowIsFinished();
        fixture.thenFinalStateIs(TestState.bothCommandsHandled());
        fixture.thenFlowStateHandlerWasCalledAsForSuccessfulFlow();
    }

    @Test
    @Timeout(3)
    void successfulFlowPass_forLoadedInProgressBusinessFlowAfterInitEventHandled_whenAllSuccessfulResponsesReturnedForRemainingSteps() {
        fixture.givenFlowIsInitialized();
        fixture.givenFlowIsProvisioned();
        fixture.givenCurrentFlowStateIsReadFromFlowStateHandler(FIRST_COMMAND_SUCCESSFUL_RESPONSE, STATE_INDEX_AFTER_INIT_EVENT_HANDLED, TestState.noCommandHandled());
        fixture.givenSecondCommandReturnsSuccessfulResponse();
        fixture.givenFinalizingCommandReturnsSuccessfulResponse();
        fixture.whenFirstCommandSuccessfulResponseIsReceived();
        fixture.thenWaitUntilFlowIsFinished();
        fixture.thenFinalStateIs(TestState.bothCommandsHandled());
        fixture.thenFlowStateHandlerWasCalledAsForSuccessfulFlow_startingWithFistCommandSuccessfulResponse();
    }

    @Test
    @Timeout(3)
    void successfulFlowPass_forLoadedInProgressBusinessFlowAfterFistCommandHandled_whenAllSuccessfulResponsesReturnedForRemainingSteps() {
        fixture.givenFlowIsInitialized();
        fixture.givenFlowIsProvisioned();
        fixture.givenCurrentFlowStateIsReadFromFlowStateHandler(SECOND_COMMAND_SUCCESSFUL_RESPONSE, STATE_INDEX_AFTER_FIRST_COMMAND_HANDLED, TestState.onlyFistCommandHandled());
        fixture.givenFinalizingCommandReturnsSuccessfulResponse();
        fixture.whenSecondCommandSuccessfulResponseIsReceived();
        fixture.thenWaitUntilFlowIsFinished();
        fixture.thenFinalStateIs(TestState.bothCommandsHandled());
        fixture.thenFlowStateHandlerWasCalledAsForSuccessfulFlow_startingWithSecondCommandSuccessfulResponse();
    }

    private static class Fixture {
        private static final String COMMAND_RESPONSE_CHANNEL_NAME = "response_channel_name";
        private static final Class<BaseTestEvent> EVENT_KIND = BaseTestEvent.class;
        private static final TestInitEvent INIT_EVENT = new TestInitEvent();

        private final BusinessFlowDefinition<TestInitEvent, TestState> businessFlowDefinition;

        @Mock private BusinessFlowStateHandler<TestState> flowStateHandler;
        @Mock private CommandChannelFactory commandChannelFactory;
        @Mock private EventListenerFactory eventListenerFactory;
        private EventListenerBuilder eventListenerBuilder;
        private BusinessFlowProvisionerConfig<TestInitEvent> flowProvisionerConfig;
        private TestState actualFlowState;
        private CommandResponseChannelMock commandResponseChannelMock;
        private ArgumentCaptor<Consumer<TestInitEvent>> initEventCaptor = ArgumentCaptor.forClass(Consumer.class);
        private BusinessFlowProvisioner<TestInitEvent, TestState> businessFlowProvisioner;


        Fixture(BusinessFlowDefinition<TestInitEvent, TestState> businessFlowDefinition, TestState flowInitialState) {
            this.businessFlowDefinition = businessFlowDefinition;
            this.actualFlowState = flowInitialState;
            MockitoAnnotations.initMocks(this);

            eventListenerBuilder = new EventListenerBuilder(eventListenerFactory);

            flowProvisionerConfig = new BusinessFlowProvisionerConfig<>(
                    COMMAND_RESPONSE_CHANNEL_NAME, EVENT_KIND, INIT_EVENT.getClass());

            businessFlowProvisioner = new DefaultBusinessFlowProvisioner<>(
                    eventListenerBuilder,
                    commandChannelFactory,
                    flowStateHandler,
                    flowProvisionerConfig
            );
        }

        void givenFlowIsInitialized() {
            setUpCommandResponseChannelMock();
            mockFlowStateHandlerToNotifyAboutPublishedCommands();
            mockEventListenerFactoryToCaptureEventListener();
        }

        private void setUpCommandResponseChannelMock() {
            commandResponseChannelMock =
                    CommandResponseChannelMock.mockCommandResponseChannel(
                            COMMAND_RESPONSE_CHANNEL_NAME, commandChannelFactory, FinishingCommandSuccessfulResponse.class
                    );
        }

        private void mockFlowStateHandlerToNotifyAboutPublishedCommands() {
            willAnswer(this::notifyCommandSent)
                    .given(flowStateHandler)
                    .createNewState(any(StateEnvelope.class), any(Command.class));
            willAnswer(this::notifyCommandSent)
                    .given(flowStateHandler)
                    .updateState(any(StateEnvelope.class), any(Command.class));
        }

        private Void notifyCommandSent(InvocationOnMock invocationOnMock) {
            commandResponseChannelMock
                    .getSentCommandNotifier()
                    .notifyCommandSent(invocationOnMock.getArgument(1, Command.class));

            return null;
        }

        private void mockEventListenerFactoryToCaptureEventListener() {
            EventListener<TestInitEvent> eventListener = mock(EventListener.class);
            given(eventListenerFactory.<TestInitEvent>listenToEventsOfKind(EVENT_KIND)).willReturn(completedFuture(eventListener));
            willDoNothing().given(eventListener).onEvent(initEventCaptor.capture());
        }


        void givenFlowIsProvisioned() {
            businessFlowProvisioner.provision(businessFlowDefinition);
        }

        void givenFirstCommandReturnsSuccessfulResponse() {
            commandResponseChannelMock.when(FIRST_COMMAND, thenRespondWith(FIRST_COMMAND_SUCCESSFUL_RESPONSE));
        }

        void givenSecondCommandReturnsSuccessfulResponse() {
            commandResponseChannelMock.when(SECOND_COMMAND, thenRespondWith(SECOND_COMMAND_SUCCESSFUL_RESPONSE));
        }

        void givenFinalizingCommandReturnsSuccessfulResponse() {

            commandResponseChannelMock.when(FINISHING_COMMAND, thenRespondWith(FINISHING_COMMAND_SUCCESSFUL_RESPONSE));
        }

        void whenFlowIsInitiatedByInitEvent() {
            initEventCaptor.getValue().accept(INIT_EVENT);
        }

        void thenWaitUntilFlowIsFinished() {
            commandResponseChannelMock
                    .getAsyncTestWaitSupport()
                    .waitUntilAsyncFlowFinished();
        }

        void thenFinalStateIs(TestState expectedTestState) {
            assertThat(actualFlowState).isEqualTo(expectedTestState);
        }

        void thenFlowStateHandlerWasCalledAsForSuccessfulFlow() {
            then(flowStateHandler).should(never()).readFlowState(any(UUID.class));
            then(flowStateHandler).should()
                    .createNewState(argThat(matchesStateWithIndexOf(0)), eq(FIRST_COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(1)), eq(SECOND_COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(2)), eq(FINISHING_COMMAND));
            then(flowStateHandler).should()
                    .finalizeState(argThat(matchesStateWithIndexOf(3)));
        }

        void thenFlowStateHandlerWasCalledAsForSuccessfulFlow_startingWithFistCommandSuccessfulResponse() {
            then(flowStateHandler).should().readFlowState(FIRST_COMMAND_SUCCESSFUL_RESPONSE.getCorrelationId());
            then(flowStateHandler).should(never())
                    .createNewState(any(StateEnvelope.class), any(Command.class));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(1)), eq(SECOND_COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(2)), eq(FINISHING_COMMAND));
            then(flowStateHandler).should()
                    .finalizeState(argThat(matchesStateWithIndexOf(3)));
        }

        void thenFlowStateHandlerWasCalledAsForSuccessfulFlow_startingWithSecondCommandSuccessfulResponse() {
            then(flowStateHandler).should().readFlowState(SECOND_COMMAND_SUCCESSFUL_RESPONSE.getCorrelationId());
            then(flowStateHandler).should(never())
                    .createNewState(any(StateEnvelope.class), any(Command.class));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(2)), eq(FINISHING_COMMAND));
            then(flowStateHandler).should()
                    .finalizeState(argThat(matchesStateWithIndexOf(3)));
        }

        private ArgumentMatcher<StateEnvelope<TestState>> matchesStateWithIndexOf(int stateIndex) {
            return actualEnvelope -> actualEnvelope.getStateIndex() == stateIndex &&
                                        actualEnvelope.getState() == actualFlowState;
        }

        void givenCurrentFlowStateIsReadFromFlowStateHandler(Response onGivenCommandResponse,
                                                             int currentStateIndex, TestState testState) {
            actualFlowState = testState;
            given(flowStateHandler.readFlowState(onGivenCommandResponse.getCorrelationId()))
                    .willReturn(StateEnvelope.recreateExistingState(currentStateIndex, testState));
        }

        void whenFirstCommandSuccessfulResponseIsReceived() {
            commandResponseChannelMock.acceptNewCommandResponseJustReceived(FIRST_COMMAND_SUCCESSFUL_RESPONSE);
        }

        void whenSecondCommandSuccessfulResponseIsReceived() {
            commandResponseChannelMock.acceptNewCommandResponseJustReceived(SECOND_COMMAND_SUCCESSFUL_RESPONSE);
        }
    }

    @ToString
    @AllArgsConstructor
    @EqualsAndHashCode
    @NoArgsConstructor(access = PRIVATE)
    private static class TestState {
        private boolean firstCommandHandled;
        private boolean secondCommandHandled;

        static TestState bothCommandsHandled() {
            return new TestState(true, true);
        }

        static TestState onlyFistCommandHandled () {
            return new TestState(true, false);
        }

        static TestState noCommandHandled() {
            return new TestState(false, false);
        }

        void firstCommandHandled() {
            firstCommandHandled = true;
        }

        void secondCommandHandled() {
            secondCommandHandled = true;
        }
    }

    private static class TestInitEvent extends BaseTestEvent {
        TestInitEvent() {
            super(BaseTestEvent.AGGREGATE_ID, BaseTestEvent.SEQUENCE_NUMBER, BaseTestEvent.GENERATED_ON);
        }
    }

    private static class InitEventCompensationCommand extends AbstractCommand {}

    @RequiredArgsConstructor(access = PROTECTED)
    private static class TestAbstractCommandResponse implements Response {
        private final UUID responseUuid;

        @Override
        public UUID getCorrelationId() {
            return responseUuid;
        }
    }


    private static class FirstCommand extends AbstractCommand {}
    private static class FirstCommandCompensation extends AbstractCommand {}
    private static class FirstCommandSuccessfulResponse extends TestAbstractCommandResponse {
        FirstCommandSuccessfulResponse(UUID responseUuid) {
            super(responseUuid);
        }
    }
    private static class FirstCommandErrorResponse extends TestAbstractCommandResponse {
        protected FirstCommandErrorResponse(UUID responseUuid) {
            super(responseUuid);
        }
    }

    private static class SecondCommand extends AbstractCommand {}
    private static class SecondCommandCompensation extends AbstractCommand {}
    private static class SecondCommandSuccessfulResponse extends TestAbstractCommandResponse {
        protected SecondCommandSuccessfulResponse(UUID responseUuid) {
            super(responseUuid);
        }
    }
    private static class SecondCommandErrorResponse extends TestAbstractCommandResponse {
        protected SecondCommandErrorResponse(UUID responseUuid) {
            super(responseUuid);
        }
    }

    private static class FinishingCommand extends AbstractCommand {}
    private static class FinishingCommandSuccessfulResponse extends TestAbstractCommandResponse {
        protected FinishingCommandSuccessfulResponse(UUID responseUuid) {
            super(responseUuid);
        }
    }
    private static class FinishingCommandErrorResponse extends TestAbstractCommandResponse {
        protected FinishingCommandErrorResponse(UUID responseUuid) {
            super(responseUuid);
        }
    }
}