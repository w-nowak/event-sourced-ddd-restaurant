package com.wnowakcraft.samples.restaurant.core.domain.logic;

import com.wnowakcraft.samples.restaurant.core.domain.logic.DefaultBusinessFlowProvisioner.BusinessFlowProvisionerConfig;
import com.wnowakcraft.samples.restaurant.core.domain.model.Command;
import com.wnowakcraft.samples.restaurant.core.domain.model.CompensationSucceededResponse;
import com.wnowakcraft.samples.restaurant.core.domain.model.ModelTestData;
import com.wnowakcraft.samples.restaurant.core.domain.model.Response;
import com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.mocking.BusinessFlowMock;
import com.wnowakcraft.samples.restaurant.core.infrastructure.saga.BusinessFlowRunner.StateEnvelope;
import com.wnowakcraft.samples.restaurant.core.infrastructure.saga.BusinessFlowStateHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

import static com.wnowakcraft.samples.restaurant.core.domain.logic.BusinessFlowDefinition.OnResponse.*;
import static com.wnowakcraft.samples.restaurant.core.domain.logic.TestData.*;
import static com.wnowakcraft.samples.restaurant.core.domain.logic.TestData.StateIndexAndCompensation.normalFlowAt;
import static com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.mocking.CommandResponseChannelMock.allowedFlowFinishedResponses;
import static com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.mocking.WhenOnCommand.ThenRespondWith.thenRespondInSequenceWith;
import static com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.mocking.WhenOnCommand.ThenRespondWith.thenRespondWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@RunWith(MockitoJUnitRunner.class)
class DefaultBusinessFlowProvisionerITTest {
    private static final int FLOW_FULLY_COMPENSATED_STATE_INDEX =  -1;
    private static final int INIT_EVENT_STATE_INDEX =  0;
    private static final int INIT_EVENT_HANDLED_STATE_INDEX = handled(INIT_EVENT_STATE_INDEX);
    private static final int FIRST_COMMAND_STATE_INDEX = 1;
    private static final int FIRST_COMMAND_HANDLED_STATE_INDEX = handled(FIRST_COMMAND_STATE_INDEX);
    private static final int QUERY_FOR_DATA_STATE_INDEX = 2;
    private static final int QUERY_FOR_DATA_HANDLED_STATE_INDEX = handled(QUERY_FOR_DATA_STATE_INDEX);
    private static final int SECOND_COMMAND_STATE_INDEX = 3;
    private static final int FINISHING_COMMAND_STATE_INDEX = 4;
    private static final int FINISHING_COMMAND_HANDLED_STATE_INDEX = handled(FINISHING_COMMAND_STATE_INDEX);

    private Fixture fixture;

    private static int handled(int stateIndex) {
        return stateIndex + 1;
    }

    @BeforeEach
    void setUp() {
        var initFlowState = TestState.noCommandHandled();

        var businessFlowDefinition =
                BusinessFlowDefinition
                        .startWith(ModelTestData.TestInitEvent.class, e -> initFlowState)
                            .compensateBy(s -> INIT_EVENT.COMPENSATION_COMMAND)
                        .thenSend(s -> FIRST_COMMAND.COMMAND)
                            .on(FirstCommandSuccessfulResponse.class, (resp, state) -> state.firstCommandHandled())
                            .on(FirstCommandErrorResponse.class, failureWithCompensation((resp, state) -> state.compensationInitiatedOnFirstCommand()))
                            .on(CompensationSucceededResponse.class, (resp, state) -> state.firstCommandCompensated())
                            .compensateBy(s -> FIRST_COMMAND.COMPENSATION)
                        .thenSend(s -> QUERY_FOR_DATA.QUERY)
                            .on(QueriedDataReturnedResponse.class, (resp, state) -> state.requestedDataIs(resp.getQueriedData()))
                            .on(QueriedDataErrorResponse.class, failureWithCompensation())
                        .thenSend(s -> SECOND_COMMAND.COMMAND)
                            .on(SecondCommandSuccessfulResponse.class, (resp, state) -> state.secondCommandHandled())
                            .on(SecondCommandErrorResponse.class, failureWithCompensation((resp, state) -> state.compensationInitiatedOnSecondCommand()))
                            .on(CompensationSucceededResponse.class, (resp, state) -> state.secondCommandCompensated())
                            .compensateBy(s -> SECOND_COMMAND.COMPENSATION)
                        .thenSend(s -> FINISHING_COMMAND.COMMAND)
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
        fixture.givenQueryForDataReturnsResponseWithRequestedData();
        fixture.givenSecondCommandReturnsSuccessfulResponse();
        fixture.givenFinalizingCommandReturnsSuccessfulResponse();
        fixture.whenFlowIsInitiatedByInitEvent();
        fixture.thenWaitUntilFlowIsFinished();
        fixture.thenFinalStateIs(
                TestState.noCommandHandled()
                        .firstCommandHandled()
                        .secondCommandHandled()
                        .requestedDataIs(QUERY_FOR_DATA.REQUESTED_DATA)
        );
        fixture.thenFlowStateHandlerWasCalledAsForSuccessfulFlow();
    }

    @Test
    @Timeout(3)
    void successfulFlowPass_forLoadedInProgressBusinessFlowResumedAfterInitEventHandled_whenAllSuccessfulResponsesReturnedForRemainingSteps() {
        fixture.givenFlowIsInitialized();
        fixture.givenFlowIsProvisioned();
        fixture.givenCurrentFlowStateIsReadFromFlowStateHandler(FIRST_COMMAND.SUCCESSFUL_RESPONSE, normalFlowAt(INIT_EVENT_HANDLED_STATE_INDEX), TestState.noCommandHandled());
        fixture.givenQueryForDataReturnsResponseWithRequestedData();
        fixture.givenSecondCommandReturnsSuccessfulResponse();
        fixture.givenFinalizingCommandReturnsSuccessfulResponse();
        fixture.whenFirstCommandSuccessfulResponseIsReceived();
        fixture.thenWaitUntilFlowIsFinished();
        fixture.thenFinalStateIs(
                TestState.noCommandHandled()
                        .firstCommandHandled()
                        .secondCommandHandled()
                        .requestedDataIs(QUERY_FOR_DATA.REQUESTED_DATA)
        );
        fixture.thenFlowStateHandlerWasCalledAsForSuccessfulFlow_startingWithFistCommandSuccessfulResponse();
    }

    @Test
    @Timeout(3)
    void successfulFlowPass_forLoadedInProgressBusinessFlowAfterFistCommandHandled_whenAllSuccessfulResponsesReturnedForRemainingSteps() {
        fixture.givenFlowIsInitialized();
        fixture.givenFlowIsProvisioned();
        fixture.givenCurrentFlowStateIsReadFromFlowStateHandler(
                QUERY_FOR_DATA.RETURNED_RESPONSE,
                normalFlowAt(FIRST_COMMAND_HANDLED_STATE_INDEX),
                TestState.noCommandHandled().firstCommandHandled()
        );
        fixture.givenSecondCommandReturnsSuccessfulResponse();
        fixture.givenFinalizingCommandReturnsSuccessfulResponse();
        fixture.whenRequestedDataIsReceived();
        fixture.thenWaitUntilFlowIsFinished();
        fixture.thenFinalStateIs(
                TestState.noCommandHandled()
                        .firstCommandHandled()
                        .secondCommandHandled()
                        .requestedDataIs(QUERY_FOR_DATA.REQUESTED_DATA)
        );
        fixture.thenFlowStateHandlerWasCalledAsForSuccessfulFlow_startingWithRequestedDataResponse();
    }

    @Test
    @Timeout(3)
    void successfulFlowPass_forLoadedInProgressBusinessFlowResumedAfterRequestedDataIsHandled_whenAllSuccessfulResponsesReturnedForRemainingSteps() {
        fixture.givenFlowIsInitialized();
        fixture.givenFlowIsProvisioned();
        fixture.givenCurrentFlowStateIsReadFromFlowStateHandler(
                SECOND_COMMAND.SUCCESSFUL_RESPONSE,
                normalFlowAt(QUERY_FOR_DATA_HANDLED_STATE_INDEX),
                TestState.noCommandHandled().firstCommandHandled().requestedDataIs(QUERY_FOR_DATA.REQUESTED_DATA)
        );
        fixture.givenFinalizingCommandReturnsSuccessfulResponse();
        fixture.whenSecondCommandSuccessfulResponseIsReceived();
        fixture.thenWaitUntilFlowIsFinished();
        fixture.thenFinalStateIs(
                TestState.noCommandHandled()
                        .firstCommandHandled()
                        .secondCommandHandled()
                        .requestedDataIs(QUERY_FOR_DATA.REQUESTED_DATA)
        );
        fixture.thenFlowStateHandlerWasCalledAsForSuccessfulFlow_startingWithSecondCommandSuccessfulResponse(); ;
    }

    @Test
    @Timeout(3)
    void compensatedFlow_whenErrorOccursAtSomePointOfResumedBusinessFlow_onStepWithCompensation_allPreviousCompensableStatesAreCompensated() {
        fixture.givenFlowIsInitialized();
        fixture.givenFlowIsProvisioned();
        fixture.givenCurrentFlowStateIsReadFromFlowStateHandler(FIRST_COMMAND.SUCCESSFUL_RESPONSE, normalFlowAt(INIT_EVENT_HANDLED_STATE_INDEX), TestState.noCommandHandled());
        fixture.givenQueryForDataReturnsResponseWithRequestedData();
        fixture.givenSecondCommandReturnsErrorResponse();
        fixture.givenSecondCommandCompensationSucceeds();
        fixture.givenFirstCommandCompensationSucceeds();
        fixture.givenInitEventCompensationSucceeds();
        fixture.whenFirstCommandSuccessfulResponseIsReceived();
        fixture.thenWaitUntilFlowIsFinished();
        fixture.thenFinalStateIs(
                TestState.noCommandHandled()
                        .firstCommandHandled()
                        .requestedDataIs(QUERY_FOR_DATA.REQUESTED_DATA)
                        .compensationInitiatedOnSecondCommand()
                        .firstCommandCompensated()
        );
        fixture.thenFlowStateHandlerWasCalledAsForCompensatedResumedFlow_whereCompensationStartedOnSecondCommand();
    }

    @Test
    @Timeout(3)
    void compensatedFlow_whenErrorOccursAtSomePointOfNewBusinessFlow_onStepWithNoCompensation_allPreviousCompensableStatesAreCompensated() {
        fixture.givenFlowIsInitialized();
        fixture.givenFlowIsProvisioned();
        fixture.givenFirstCommandReturnsSuccessfulResponse();
        fixture.givenQueryForDataReturnsErrorResponse();
        fixture.givenFirstCommandCompensationSucceeds();
        fixture.givenInitEventCompensationSucceeds();
        fixture.whenFlowIsInitiatedByInitEvent();
        fixture.thenWaitUntilFlowIsFinished();
        fixture.thenFinalStateIs(
                TestState.noCommandHandled()
                        .firstCommandHandled()
                        .firstCommandCompensated()
        );
        fixture.thenFlowStateHandlerWasCalledAsForCompensatedNewFlow_whereCompensationStartedOnQueryForData();
    }

    @Test
    @Timeout(3)
    void successfulFlowPass_withRetriesOnRetryableStep_whenRetryUltimatelySucceeds_andAllSuccessfulResponsesReturned() {
        fixture.givenFlowIsInitialized();
        fixture.givenFlowIsProvisioned();
        fixture.givenFirstCommandReturnsSuccessfulResponse();
        fixture.givenQueryForDataReturnsResponseWithRequestedData();
        fixture.givenSecondCommandReturnsSuccessfulResponse();
        fixture.givenFinalizingCommandReturnsSuccessfulResponseOnThirdRetry();
        fixture.whenFlowIsInitiatedByInitEvent();
        fixture.thenWaitUntilFlowIsFinished();
        fixture.thenFinalStateIs(
                TestState.noCommandHandled()
                        .firstCommandHandled()
                        .secondCommandHandled()
                        .requestedDataIs(QUERY_FOR_DATA.REQUESTED_DATA)
        );
        fixture.thenFlowStateHandlerWasCalledAsForSuccessfulFlow_withThreeRetriesOnFinalizingCommand_andAllOtherCommandsSuccessfulResponses();
    }

    private static class Fixture {
        private static final String COMMAND_RESPONSE_CHANNEL_NAME = "response_channel_name";

        private final BusinessFlowDefinition<ModelTestData.TestInitEvent, TestState> businessFlowDefinition;

        private BusinessFlowStateHandler<TestState> flowStateHandler;
        private BusinessFlowProvisionerConfig<ModelTestData.TestInitEvent> flowProvisionerConfig;
        private TestState actualFlowState;
        private BusinessFlowProvisioner<ModelTestData.TestInitEvent, TestState> businessFlowProvisioner;
        private BusinessFlowMock<ModelTestData.TestInitEvent, TestState> businessFlowMock;

        Fixture(BusinessFlowDefinition<ModelTestData.TestInitEvent, TestState> businessFlowDefinition, TestState flowInitialState) {
            this.businessFlowDefinition = businessFlowDefinition;
            this.actualFlowState = flowInitialState;
            MockitoAnnotations.initMocks(this);

            flowProvisionerConfig = new BusinessFlowProvisionerConfig<>(
                    COMMAND_RESPONSE_CHANNEL_NAME, EVENT_FAMILY, INIT_EVENT.EVENT.getClass());

            businessFlowMock = new BusinessFlowMock<>(
                    flowProvisionerConfig,
                    allowedFlowFinishedResponses(FinishingCommandSuccessfulResponse.class, INIT_EVENT.COMPENSATION_SUCCEEDED_RESPONSE.getClass())
            );

            this.flowStateHandler = businessFlowMock.getFlowStateHandler();
        }

        void givenFlowIsInitialized() {
            businessFlowProvisioner = businessFlowMock.initializeTestProvisioner(DefaultBusinessFlowProvisioner::new);
        }

        void givenFlowIsProvisioned() {
            businessFlowProvisioner.provision(businessFlowDefinition);
        }

        void givenFirstCommandReturnsSuccessfulResponse() {
            businessFlowMock.getOnCommandMock().when(FIRST_COMMAND.COMMAND.getClass(), thenRespondWith(FIRST_COMMAND.SUCCESSFUL_RESPONSE));
        }

        void givenQueryForDataReturnsResponseWithRequestedData() {
            businessFlowMock.getOnCommandMock().when(QUERY_FOR_DATA.QUERY.getClass(), thenRespondWith(QUERY_FOR_DATA.RETURNED_RESPONSE));
        }

        void givenQueryForDataReturnsErrorResponse() {
            businessFlowMock.getOnCommandMock().when(QUERY_FOR_DATA.QUERY.getClass(), thenRespondWith(QUERY_FOR_DATA.ERROR_RESPONSE));
        }

        void givenSecondCommandReturnsSuccessfulResponse() {
            businessFlowMock.getOnCommandMock().when(SECOND_COMMAND.COMMAND.getClass(), thenRespondWith(SECOND_COMMAND.SUCCESSFUL_RESPONSE));
        }

        void givenSecondCommandReturnsErrorResponse() {
            businessFlowMock.getOnCommandMock().when(SECOND_COMMAND.COMMAND.getClass(), thenRespondWith(SECOND_COMMAND.ERROR_RESPONSE));
        }

        void givenFinalizingCommandReturnsSuccessfulResponse() {
            businessFlowMock.getOnCommandMock().when(FINISHING_COMMAND.COMMAND.getClass(), thenRespondWith(FINISHING_COMMAND.SUCCESSFUL_RESPONSE));
        }

        void givenFinalizingCommandReturnsSuccessfulResponseOnThirdRetry() {
            businessFlowMock.getOnCommandMock().when(
                    FINISHING_COMMAND.COMMAND.getClass(),
                    thenRespondInSequenceWith(
                            FINISHING_COMMAND.ERROR_RESPONSE,
                            FINISHING_COMMAND.ERROR_RESPONSE,
                            FINISHING_COMMAND.ERROR_RESPONSE,
                            FINISHING_COMMAND.SUCCESSFUL_RESPONSE)
            );
        }

        void givenSecondCommandCompensationSucceeds() {
            businessFlowMock.getOnCommandMock().when(SECOND_COMMAND.COMPENSATION.getClass(), thenRespondWith(SECOND_COMMAND.COMPENSATION_SUCCEEDED_RESPONSE));
        }

        void givenFirstCommandCompensationSucceeds() {
            businessFlowMock.getOnCommandMock().when(FIRST_COMMAND.COMPENSATION.getClass(), thenRespondWith(FIRST_COMMAND.COMPENSATION_SUCCEEDED_RESPONSE));
        }

        void givenInitEventCompensationSucceeds() {
            businessFlowMock.getOnCommandMock().when(INIT_EVENT.COMPENSATION_COMMAND.getClass(), thenRespondWith(INIT_EVENT.COMPENSATION_SUCCEEDED_RESPONSE));
        }

        void whenFlowIsInitiatedByInitEvent() {
            businessFlowMock.triggerBusinessFlowInitEvent(INIT_EVENT.EVENT);
        }

        void thenWaitUntilFlowIsFinished() {
            businessFlowMock.thenWaitUntilFlowIsFinished();
        }

        void thenFinalStateIs(TestState expectedTestState) {
            assertThat(actualFlowState).isEqualTo(expectedTestState);
        }

        void thenFlowStateHandlerWasCalledAsForSuccessfulFlow() {
            then(flowStateHandler).should(never()).readFlowState(any(UUID.class));
            then(flowStateHandler).should()
                    .createNewState(argThat(matchesStateWithIndexOf(FIRST_COMMAND_STATE_INDEX)), eq(FIRST_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(QUERY_FOR_DATA_STATE_INDEX)), eq(QUERY_FOR_DATA.QUERY));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(SECOND_COMMAND_STATE_INDEX)), eq(SECOND_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(FINISHING_COMMAND_STATE_INDEX)), eq(FINISHING_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .finalizeState(argThat(matchesStateWithIndexOf(FINISHING_COMMAND_HANDLED_STATE_INDEX)));
        }

        void thenFlowStateHandlerWasCalledAsForSuccessfulFlow_startingWithFistCommandSuccessfulResponse() {
            then(flowStateHandler).should().readFlowState(FIRST_COMMAND.SUCCESSFUL_RESPONSE.getCorrelationId());
            then(flowStateHandler).should(never())
                    .createNewState(anyStateEnvelope(), any(Command.class));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(FIRST_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(QUERY_FOR_DATA_STATE_INDEX)), eq(QUERY_FOR_DATA.QUERY));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(SECOND_COMMAND_STATE_INDEX)), eq(SECOND_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(FINISHING_COMMAND_STATE_INDEX)), eq(FINISHING_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .finalizeState(argThat(matchesStateWithIndexOf(FINISHING_COMMAND_HANDLED_STATE_INDEX)));
        }

        @SuppressWarnings("unchecked")
        private static StateEnvelope<TestState> anyStateEnvelope() {
            return any(StateEnvelope.class);
        }

        void thenFlowStateHandlerWasCalledAsForSuccessfulFlow_startingWithRequestedDataResponse() {
            then(flowStateHandler).should().readFlowState(QUERY_FOR_DATA.RETURNED_RESPONSE.getCorrelationId());
            then(flowStateHandler).should(never())
                    .createNewState(anyStateEnvelope(), any(Command.class));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(FIRST_COMMAND.COMMAND));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(QUERY_FOR_DATA.QUERY));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(SECOND_COMMAND_STATE_INDEX)), eq(SECOND_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(FINISHING_COMMAND_STATE_INDEX)), eq(FINISHING_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .finalizeState(argThat(matchesStateWithIndexOf(FINISHING_COMMAND_HANDLED_STATE_INDEX)));
        }

        void thenFlowStateHandlerWasCalledAsForSuccessfulFlow_startingWithSecondCommandSuccessfulResponse() {
            then(flowStateHandler).should().readFlowState(SECOND_COMMAND.SUCCESSFUL_RESPONSE.getCorrelationId());
            then(flowStateHandler).should(never())
                    .createNewState(anyStateEnvelope(), any(Command.class));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(FIRST_COMMAND.COMMAND));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(QUERY_FOR_DATA.QUERY));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(SECOND_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(FINISHING_COMMAND_STATE_INDEX)), eq(FINISHING_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .finalizeState(argThat(matchesStateWithIndexOf(FINISHING_COMMAND_HANDLED_STATE_INDEX)));
        }

        void thenFlowStateHandlerWasCalledAsForCompensatedResumedFlow_whereCompensationStartedOnSecondCommand() {
            then(flowStateHandler).should().readFlowState(FIRST_COMMAND.SUCCESSFUL_RESPONSE.getCorrelationId());
            then(flowStateHandler).should(never())
                    .createNewState(anyStateEnvelope(), any(Command.class));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(FIRST_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(QUERY_FOR_DATA_STATE_INDEX)), eq(QUERY_FOR_DATA.QUERY));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(SECOND_COMMAND_STATE_INDEX)), eq(SECOND_COMMAND.COMMAND));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(FINISHING_COMMAND.COMMAND));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(SECOND_COMMAND.COMPENSATION));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(FIRST_COMMAND_STATE_INDEX)), eq(FIRST_COMMAND.COMPENSATION));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(INIT_EVENT_STATE_INDEX)), eq(INIT_EVENT.COMPENSATION_COMMAND));
            then(flowStateHandler).should()
                    .finalizeState(argThat(matchesStateWithIndexOf(FLOW_FULLY_COMPENSATED_STATE_INDEX)));
        }

        void thenFlowStateHandlerWasCalledAsForCompensatedNewFlow_whereCompensationStartedOnQueryForData() {
            then(flowStateHandler).should(never()).readFlowState(any(UUID.class));
            then(flowStateHandler).should()
                    .createNewState(argThat(matchesStateWithIndexOf(FIRST_COMMAND_STATE_INDEX)), eq(FIRST_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(QUERY_FOR_DATA_STATE_INDEX)), eq(QUERY_FOR_DATA.QUERY));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(SECOND_COMMAND.COMMAND));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(FINISHING_COMMAND.COMMAND));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(SECOND_COMMAND.COMPENSATION));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(FIRST_COMMAND_STATE_INDEX)), eq(FIRST_COMMAND.COMPENSATION));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(INIT_EVENT_STATE_INDEX)), eq(INIT_EVENT.COMPENSATION_COMMAND));
            then(flowStateHandler).should()
                    .finalizeState(argThat(matchesStateWithIndexOf(FLOW_FULLY_COMPENSATED_STATE_INDEX)));
        }

        void thenFlowStateHandlerWasCalledAsForSuccessfulFlow_withThreeRetriesOnFinalizingCommand_andAllOtherCommandsSuccessfulResponses() {
            then(flowStateHandler).should(never()).readFlowState(any(UUID.class));
            then(flowStateHandler).should()
                    .createNewState(argThat(matchesStateWithIndexOf(FIRST_COMMAND_STATE_INDEX)), eq(FIRST_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(QUERY_FOR_DATA_STATE_INDEX)), eq(QUERY_FOR_DATA.QUERY));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(SECOND_COMMAND_STATE_INDEX)), eq(SECOND_COMMAND.COMMAND));
            then(flowStateHandler).should(times(4))
                    .updateState(argThat(matchesStateWithIndexOf(FINISHING_COMMAND_STATE_INDEX)), eq(FINISHING_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .finalizeState(argThat(matchesStateWithIndexOf(FINISHING_COMMAND_HANDLED_STATE_INDEX)));
        }

        private ArgumentMatcher<StateEnvelope<TestState>> matchesStateWithIndexOf(int stateIndex) {
            return actualEnvelope -> actualEnvelope.getStateIndex() == stateIndex &&
                                        actualEnvelope.getState().equals(actualFlowState);
        }

        void givenCurrentFlowStateIsReadFromFlowStateHandler(Response onGivenCommandResponse,
                                                             StateIndexAndCompensation stateIndexAndCompensation,
                                                             TestState testState) {
            actualFlowState = testState;
            given(flowStateHandler.readFlowState(onGivenCommandResponse.getCorrelationId()))
                    .willReturn(StateEnvelope.recreateExistingState(stateIndexAndCompensation.getIndex(), testState, stateIndexAndCompensation.isCompensation()));
        }

        void whenFirstCommandSuccessfulResponseIsReceived() {
            businessFlowMock.whenFollowingCommandResponseReceived(FIRST_COMMAND.SUCCESSFUL_RESPONSE);
        }

        void whenSecondCommandSuccessfulResponseIsReceived() {
            businessFlowMock.whenFollowingCommandResponseReceived(SECOND_COMMAND.SUCCESSFUL_RESPONSE);
        }

        void whenRequestedDataIsReceived() {
            businessFlowMock.whenFollowingCommandResponseReceived(QUERY_FOR_DATA.RETURNED_RESPONSE);
        }
    }
}