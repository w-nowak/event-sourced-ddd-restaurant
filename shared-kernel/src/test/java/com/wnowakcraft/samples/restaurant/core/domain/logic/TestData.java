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
        static final Response COMPENSATION_SUCCEEDED_RESPONSE = new InitEventCompensationResponse(UUID.randomUUID());
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
        static final QueriedDataErrorResponse ERROR_RESPONSE = new QueriedDataErrorResponse(UUID.randomUUID());
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
        private boolean firstCommandHandled;
        private boolean secondCommandHandled;
        private boolean firstCommandCompensated;
        private boolean secondCommandCompensated;
        private boolean compensationInitiatedOnFistCommand;
        private boolean compensationInitiatedOnQueryForData;
        private boolean compensationInitiatedOnSecondCommand;
        private String requestedData;

        static TestState noCommandHandled() {
            return new TestState();
        }

        TestState firstCommandHandled() {
            firstCommandHandled = true;
            return this;
        }

        TestState firstCommandCompensated() {
            firstCommandCompensated = true;
            return this;
        }

        TestState secondCommandHandled() {
            secondCommandHandled = true;
            return this;
        }

        TestState secondCommandCompensated() {
            secondCommandCompensated = true;
            return this;
        }

        TestState requestedDataIs(String requestedData) {
            this.requestedData = requestedData;
            return this;
        }

        TestState compensationInitiatedOnFirstCommand() {
            this.compensationInitiatedOnFistCommand = true;
            return this;
        }

        TestState compensationInitiatedOnQueryForData() {
            this.compensationInitiatedOnQueryForData = true;
            return this;
        }


        TestState compensationInitiatedOnSecondCommand() {
            this.compensationInitiatedOnSecondCommand = true;
            return this;
        }
    }

    static class TestInitEvent extends BaseTestEvent {
        TestInitEvent() {
            super(BaseTestEvent.AGGREGATE_ID, BaseTestEvent.SEQUENCE_NUMBER, BaseTestEvent.GENERATED_ON);
        }
    }

    private static class InitEventCompensationCommand extends AbstractCommand {}

    static class InitEventCompensationResponse extends TestAbstractCommandResponse {
        InitEventCompensationResponse(UUID responseUuid) {
            super(responseUuid);
        }
    }

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

    static class QueriedDataErrorResponse extends TestAbstractCommandResponse {
        protected QueriedDataErrorResponse(UUID responseUuid) {
            super(responseUuid);
        }
    }
}
