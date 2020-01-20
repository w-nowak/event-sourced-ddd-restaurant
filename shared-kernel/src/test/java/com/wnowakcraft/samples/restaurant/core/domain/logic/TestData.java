package com.wnowakcraft.samples.restaurant.core.domain.logic;

import com.wnowakcraft.samples.restaurant.core.domain.model.*;
import lombok.*;

import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

public class TestData {
    static final Class<BaseTestEvent> EVENT_FAMILY = BaseTestEvent.class;

    static class INIT_EVENT {
        static final TestInitEvent EVENT = new TestInitEvent();
        static final Command COMPENSATION_COMMAND = new InitEventCompensationCommand();
        static final CompensationCommandSucceededResponse COMPENSATION_SUCCEEDED_RESPONSE = new CompensationCommandSucceededResponse(UUID.randomUUID());
    }

    static class FIRST_COMMAND {
        static final Command COMMAND = new FirstCommand();
        static final Command COMPENSATION = new FirstCommandCompensation();
        static final FirstCommandSuccessfulResponse SUCCESSFUL_RESPONSE = new FirstCommandSuccessfulResponse(UUID.randomUUID());
        static final CompensationCommandSucceededResponse COMPENSATION_SUCCEEDED_RESPONSE = new CompensationCommandSucceededResponse(UUID.randomUUID());
    }

    static class QUERY_FOR_DATA {
        static final Query QUERY = new QueryForData();
        static final String REQUESTED_DATA = "requested string data";
        static final QueriedDataReturnedResponse RETURNED_RESPONSE = new QueriedDataReturnedResponse(UUID.randomUUID(), REQUESTED_DATA);
    }

    static class SECOND_COMMAND {
        static final Command COMMAND = new SecondCommand();
        static final Command COMPENSATION = new SecondCommandCompensation();
        static final SecondCommandSuccessfulResponse SUCCESSFUL_RESPONSE = new SecondCommandSuccessfulResponse(UUID.randomUUID());
        static final SecondCommandErrorResponse ERROR_RESPONSE = new SecondCommandErrorResponse(UUID.randomUUID());
        static final CompensationCommandSucceededResponse COMPENSATION_SUCCEEDED_RESPONSE = new CompensationCommandSucceededResponse(UUID.randomUUID());
    }

    static class FINISHING_COMMAND {
        static final Command COMMAND = new FinishingCommand();
        static final FinishingCommandSuccessfulResponse SUCCESSFUL_RESPONSE = new FinishingCommandSuccessfulResponse(UUID.randomUUID());
        static final FinishingCommandErrorResponse ERROR_RESPONSE = new FinishingCommandErrorResponse(UUID.randomUUID());
    }


    @Value
    static class StateIndexAndCompensation {
        private static final boolean IS_COMPENSATION_FLOW = true;
        private static final boolean IS_NORMAL_FLOW = false;
        private int index;
        private boolean compensation;

        static StateIndexAndCompensation normalFlowAt(int index) {
            return new StateIndexAndCompensation(index, IS_NORMAL_FLOW);
        }

        static StateIndexAndCompensation compensationFlowAt(int index) {
            return new StateIndexAndCompensation(index, IS_COMPENSATION_FLOW);
        }
    }

    @ToString
    @AllArgsConstructor
    @EqualsAndHashCode
    @NoArgsConstructor(access = PRIVATE)
    static class TestState {
        private final static String NO_QUERIED_DATA = null;
        private static final boolean FIRST_COMMAND_HANDLED = true;
        private static final boolean FIRST_COMMAND_NOT_HANDLED = false;
        private static final boolean SECOND_COMMAND_HANDLED = true;
        private static final boolean SECOND_COMMAND_NOT_HANDLED = false;
        private static final boolean FIRST_COMMAND_COMPENSATED = true;
        private static final boolean FIRST_COMMAND_NOT_COMPENSATED = false;
        private static final boolean SECOND_COMMAND_NOT_COMPENSATED = false;
        public static final boolean COMPENSATION_NOT_INITIATED_ON_FIST_COMMAND = false;
        private static final boolean COMPENSATION_INITIATED_ON_SECOND_COMMAND = true;
        private static final boolean COMPENSATION_NOT_INITIATED_ON_SECOND_COMMAND = false;

        private boolean firstCommandHandled;
        private boolean secondCommandHandled;
        private boolean firstCommandCompensated;
        private boolean secondCommandCompensated;
        private boolean compensationInitiatedOnFistCommand;
        private boolean compensationInitiatedOnSecondCommand;
        private String requestedData;

        static TestState bothCommandsHandledAndRequestedDataSetWith(String requestedData) {
            return new TestState(FIRST_COMMAND_HANDLED, SECOND_COMMAND_HANDLED, FIRST_COMMAND_NOT_COMPENSATED, SECOND_COMMAND_NOT_COMPENSATED, COMPENSATION_NOT_INITIATED_ON_FIST_COMMAND, COMPENSATION_NOT_INITIATED_ON_SECOND_COMMAND, requestedData);
        }

        static TestState onlyFistCommandHandled () {
            return new TestState(FIRST_COMMAND_HANDLED, SECOND_COMMAND_NOT_HANDLED, FIRST_COMMAND_NOT_COMPENSATED, SECOND_COMMAND_NOT_COMPENSATED, COMPENSATION_NOT_INITIATED_ON_FIST_COMMAND, COMPENSATION_NOT_INITIATED_ON_SECOND_COMMAND, NO_QUERIED_DATA);
        }

        static TestState fistCommandHandledAndRequestedDataIs(String requestedData) {
            return new TestState(FIRST_COMMAND_HANDLED, SECOND_COMMAND_NOT_HANDLED, FIRST_COMMAND_NOT_COMPENSATED, SECOND_COMMAND_NOT_COMPENSATED, COMPENSATION_NOT_INITIATED_ON_FIST_COMMAND, COMPENSATION_NOT_INITIATED_ON_SECOND_COMMAND, requestedData);
        }

        static TestState noCommandHandled() {
            return new TestState(FIRST_COMMAND_NOT_HANDLED, SECOND_COMMAND_NOT_HANDLED, FIRST_COMMAND_NOT_COMPENSATED, SECOND_COMMAND_NOT_COMPENSATED, COMPENSATION_NOT_INITIATED_ON_FIST_COMMAND, COMPENSATION_NOT_INITIATED_ON_SECOND_COMMAND, NO_QUERIED_DATA);
        }

        static TestState stateStartedOnFirstCommandCompensatedOnSecondWithRequestedDataSetWith(String requestedData) {
            return new TestState(FIRST_COMMAND_HANDLED, SECOND_COMMAND_NOT_HANDLED, FIRST_COMMAND_COMPENSATED, SECOND_COMMAND_NOT_COMPENSATED, COMPENSATION_NOT_INITIATED_ON_FIST_COMMAND, COMPENSATION_INITIATED_ON_SECOND_COMMAND, requestedData);
        }

        void firstCommandHandled() {
            firstCommandHandled = true;
        }

        void firstCommandCompensated() {
            firstCommandCompensated = true;
        }

        void secondCommandHandled() {
            secondCommandHandled = true;
        }

        void secondCommandCompensated() {
            secondCommandCompensated = true;
        }

        void requestedDataIs(String requestedData) {
            this.requestedData = requestedData;
        }

        void compensationInitiatedOnFirstCommand() {
            this.compensationInitiatedOnFistCommand = true;
        }

        void compensationInitiatedOnSecondCommand() {
            this.compensationInitiatedOnSecondCommand = true;
        }
    }

    static class TestInitEvent extends BaseTestEvent {
        TestInitEvent() {
            super(BaseTestEvent.AGGREGATE_ID, BaseTestEvent.SEQUENCE_NUMBER, BaseTestEvent.GENERATED_ON);
        }
    }

    private static class InitEventCompensationCommand extends AbstractCommand {}

    @RequiredArgsConstructor(access = PROTECTED)
    static class TestAbstractCommandResponse implements Response {
        private final UUID responseUuid;

        @Override
        public UUID getCorrelationId() {
            return responseUuid;
        }
    }

    private static class FirstCommand extends AbstractCommand {}
    private static class FirstCommandCompensation extends AbstractCommand {}

    static class FirstCommandSuccessfulResponse extends TestAbstractCommandResponse {
        FirstCommandSuccessfulResponse(UUID responseUuid) {
            super(responseUuid);
        }
    }

    static class FirstCommandErrorResponse extends TestAbstractCommandResponse {
        protected FirstCommandErrorResponse(UUID responseUuid) {
            super(responseUuid);
        }
    }

    private static class SecondCommand extends AbstractCommand {}
    private static class SecondCommandCompensation extends AbstractCommand {}

    static class SecondCommandSuccessfulResponse extends TestAbstractCommandResponse {
        protected SecondCommandSuccessfulResponse(UUID responseUuid) {
            super(responseUuid);
        }
    }

    static class SecondCommandErrorResponse extends TestAbstractCommandResponse {
        protected SecondCommandErrorResponse(UUID responseUuid) {
            super(responseUuid);
        }
    }

    static class FinishingCommand extends AbstractCommand {}

    static class FinishingCommandSuccessfulResponse extends TestAbstractCommandResponse {
        protected FinishingCommandSuccessfulResponse(UUID responseUuid) {
            super(responseUuid);
        }
    }

    static class FinishingCommandErrorResponse extends TestAbstractCommandResponse {
        protected FinishingCommandErrorResponse(UUID responseUuid) {
            super(responseUuid);
        }
    }

    static class CompensationCommandSucceededResponse extends TestAbstractCommandResponse implements CompensationSucceededResponse {
        protected CompensationCommandSucceededResponse(UUID responseUuid) {
            super(responseUuid);
        }
    }

    private static class QueryForData extends AbstractQuery {}

    @Getter
    static class QueriedDataReturnedResponse extends AbstractResponse {
        private final String queriedData;
        protected QueriedDataReturnedResponse(UUID responseUuid, String queriedData) {
            super(responseUuid);
            this.queriedData = queriedData;
        }
    }
}
