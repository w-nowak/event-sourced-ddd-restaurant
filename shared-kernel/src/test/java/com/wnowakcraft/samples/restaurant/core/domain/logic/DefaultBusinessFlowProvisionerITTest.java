package com.wnowakcraft.samples.restaurant.core.domain.logic;

import com.wnowakcraft.samples.restaurant.core.domain.logic.DefaultBusinessFlowProvisioner.BusinessFlowProvisionerConfig;
import com.wnowakcraft.samples.restaurant.core.domain.model.*;
import com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.*;
import com.wnowakcraft.samples.restaurant.core.infrastructure.saga.BusinessFlowRunner.StateEnvelope;
import com.wnowakcraft.samples.restaurant.core.infrastructure.saga.BusinessFlowStateHandler;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
class DefaultBusinessFlowProvisionerITTest {
    private static final FirstCommand FIRST_COMMAND = new FirstCommand();
    private static final SecondCommand SECOND_COMMAND = new SecondCommand();
    private static final FinishingCommand FINISHING_COMMAND = new FinishingCommand();

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

    private static class Fixture {
        private static final String COMMAND_RESPONSE_CHANNEL_NAME = "response_channel_name";
        private static final Class<BaseTestEvent> EVENT_KIND = BaseTestEvent.class;
        private static final TestInitEvent INIT_EVENT = new TestInitEvent();

        @Mock private BusinessFlowStateHandler<TestState> flowStateHandler;
        @Mock private CommandChannelFactory commandChannelFactory;
        @Mock private EventListenerFactory eventListenerFactory;
        private EventListenerBuilder eventListenerBuilder;
        private BusinessFlowProvisionerConfig<TestInitEvent> flowProvisionerConfig;
        private final BusinessFlowDefinition<TestInitEvent, TestState> businessFlowDefinition;
        private final TestState actualFlowState;
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
            commandResponseChannelMock.when(FIRST_COMMAND, thenRespondWith(new FirstCommandSuccessfulResponse()));
        }

        void givenSecondCommandReturnsSuccessfulResponse() {
            commandResponseChannelMock.when(SECOND_COMMAND, thenRespondWith(new SecondCommandSuccessfulResponse()));
        }

        void givenFinalizingCommandReturnsSuccessfulResponse() {
            commandResponseChannelMock.when(FINISHING_COMMAND, thenRespondWith(new FinishingCommandSuccessfulResponse()));
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
            then(flowStateHandler).should(never()).createNewState(argThat(matchesStateWithIndexOf(1)), eq(FIRST_COMMAND));
            then(flowStateHandler).should(never()).updateState(argThat(matchesStateWithIndexOf(2)), eq(SECOND_COMMAND));
            then(flowStateHandler).should(never()).updateState(argThat(matchesStateWithIndexOf(3)), eq(FINISHING_COMMAND));
            then(flowStateHandler).should(never()).finalizeState(argThat(matchesStateWithIndexOf(4)));
        }

        private ArgumentMatcher<StateEnvelope<TestState>> matchesStateWithIndexOf(int stateIndex) {
            return actualEnvelope -> actualEnvelope.getStateIndex() == stateIndex &&
                                        actualEnvelope.getState() == actualFlowState;
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    private static class TestState {
        private boolean firstCommmandHandled;
        private boolean secondCommmandHandled;

        static TestState bothCommandsHandled() {
            return new TestState(true, true);
        }

        void firstCommandHandled() {
            firstCommmandHandled = true;
        }

        void secondCommandHandled() {
            secondCommmandHandled = true;
        }
    }

    private static class TestInitEvent extends BaseTestEvent {
        TestInitEvent() {
            super(BaseTestEvent.AGGREGATE_ID, BaseTestEvent.SEQUENCE_NUMBER, BaseTestEvent.GENERATED_ON);
        }
    }

    private static class InitEventCompensationCommand extends AbstractCommand {}

    private static class TestAbstractCommandResponse implements Response {
        private static final UUID RESPONSE_UUID = UUID.randomUUID();

        @Override
        public UUID getCorrelationId() {
            return RESPONSE_UUID;
        }
    }


    private static class FirstCommand extends AbstractCommand {}
    private static class FirstCommandCompensation extends AbstractCommand {}
    private static class FirstCommandSuccessfulResponse extends TestAbstractCommandResponse {}
    private static class FirstCommandErrorResponse extends TestAbstractCommandResponse {}

    private static class SecondCommand extends AbstractCommand {}
    private static class SecondCommandCompensation extends AbstractCommand {}
    private static class SecondCommandSuccessfulResponse extends TestAbstractCommandResponse {}
    private static class SecondCommandErrorResponse extends TestAbstractCommandResponse {}

    private static class FinishingCommand extends AbstractCommand {}
    private static class FinishingCommandSuccessfulResponse extends TestAbstractCommandResponse {}
    private static class FinishingCommandErrorResponse extends TestAbstractCommandResponse {}
}