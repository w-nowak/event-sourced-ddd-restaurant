package com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.mocking;

import com.wnowakcraft.samples.restaurant.core.domain.logic.BusinessFlowProvisioner;
import com.wnowakcraft.samples.restaurant.core.domain.logic.DefaultBusinessFlowProvisioner.BusinessFlowProvisionerConfig;
import com.wnowakcraft.samples.restaurant.core.domain.model.Command;
import com.wnowakcraft.samples.restaurant.core.domain.model.Event;
import com.wnowakcraft.samples.restaurant.core.domain.model.Response;
import com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.CommandChannelFactory;
import com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.EventListener;
import com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.EventListenerBuilder;
import com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.EventListenerFactory;
import com.wnowakcraft.samples.restaurant.core.infrastructure.saga.BusinessFlowRunner;
import com.wnowakcraft.samples.restaurant.core.infrastructure.saga.BusinessFlowStateHandler;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

public class BusinessFlowMock<E extends Event<?>, S> {
    private static final BeforeCommandSent NO_BEFORE_COMMAND_SENT_HANDLER = (c) -> {};
    private final BusinessFlowStateHandler<S> flowStateHandler;
    private final CommandChannelFactory commandChannelFactory;
    private final EventListenerFactory eventListenerFactory;
    private CommandResponseChannelMock commandResponseChannelMock;
    private ArgumentCaptor<Consumer<E>> initEventCaptor = ArgumentCaptor.forClass(Consumer.class);
    private final BusinessFlowProvisionerConfig<E> flowProvisionerConfig;
    private Collection<Class<? extends Response>> flowFinishedResponseTypes;
    private BeforeCommandSent beforeCommandSentHandler = NO_BEFORE_COMMAND_SENT_HANDLER;

    public BusinessFlowMock(BusinessFlowProvisionerConfig<E> flowProvisionerConfig,
                            Collection<Class<? extends Response>> flowFinishedResponseTypes) {
        this.flowProvisionerConfig = flowProvisionerConfig;
        this.flowFinishedResponseTypes = flowFinishedResponseTypes;

        flowStateHandler = mock(BusinessFlowStateHandler.class);
        commandChannelFactory = mock(CommandChannelFactory.class);
        eventListenerFactory = mock(EventListenerFactory.class);

    }

    @FunctionalInterface
    public interface TestProvisionerFactory<E extends Event<?>, S> {
        BusinessFlowProvisioner<E, S> initialize(EventListenerBuilder eventListenerBuilder,
                                                 CommandChannelFactory commandChannelFactory,
                                                 BusinessFlowStateHandler<S> businessFlowStateHandler,
                                                 BusinessFlowProvisionerConfig<E> businessFlowProvisionerConfig);
    }

    public BusinessFlowProvisioner<E, S> initializeTestProvisioner(TestProvisionerFactory<E, S> testProvisionerFactory) {

        var businessFlowProvisioner = testProvisionerFactory.initialize(
                new EventListenerBuilder(eventListenerFactory),
                commandChannelFactory,
                flowStateHandler,
                flowProvisionerConfig
        );

        mockEventListenerFactoryToCaptureEventListener(flowProvisionerConfig.getEventKindToListenTo());
        mockFlowStateHandlerToNotifyAboutPublishedCommands();

        commandResponseChannelMock =createCommandResponseChannelMock(
                commandChannelFactory,
                flowProvisionerConfig.getCommandResponseChannelName(),
                flowFinishedResponseTypes);

        return businessFlowProvisioner;
    }

    public void triggerBusinessFlowInitEvent(E initEvent) {
        initEventCaptor.getValue().accept(initEvent);

        commandResponseChannelMock
                .getAsyncTestWaitSupport()
                .waitUntilAsyncFlowFinished();
    }

    @SafeVarargs
    public static Collection<Class<? extends Response>> allowedFlowFinishedResponses(Class<? extends Response>... responses) {
        return List.of(responses);
    }

    public WhenOnCommand getWhenOnCommandMock() {
        return commandResponseChannelMock;
    }

    private void mockFlowStateHandlerToNotifyAboutPublishedCommands() {
        willAnswer(this::notifyCommandSent)
                .given(flowStateHandler)
                .createNewState(anyStateEnvelope(), any(Command.class));
        willAnswer(this::notifyCommandSent)
                .given(flowStateHandler)
                .updateState(anyStateEnvelope(), any(Command.class));
    }

    private Void notifyCommandSent(InvocationOnMock invocationOnMock) {
        var command =invocationOnMock.getArgument(1, Command.class);

        beforeCommandSentHandler.beforeCommandSent(command);

        commandResponseChannelMock
                .getSentCommandNotifier()
                .notifyCommandSent(command);

        return null;
    }

    @SuppressWarnings("unchecked")
    private static <S> BusinessFlowRunner.StateEnvelope<S> anyStateEnvelope() {
        return any(BusinessFlowRunner.StateEnvelope.class);
    }


    private static CommandResponseChannelMock createCommandResponseChannelMock(CommandChannelFactory commandChannelFactory,
                                                                               String commandResponseChannelName,
                                                                               Collection<Class<? extends Response>> flowFinishedResponseTypes) {
        return CommandResponseChannelMock.mockCommandResponseChannel(
                commandResponseChannelName, commandChannelFactory,
                flowFinishedResponseTypes
        );
    }

    private void mockEventListenerFactoryToCaptureEventListener(Class<? super E> eventFamily) {
        EventListener<E> eventListener = mock(EventListener.class);
        given(eventListenerFactory.<E>listenToEventsOfKind(eventFamily)).willReturn(completedFuture(eventListener));
        willDoNothing().given(eventListener).onEvent(initEventCaptor.capture());
    }

    public void attachBeforeCommandSentHandler(BeforeCommandSent beforeCommandSentHandler) {
        this.beforeCommandSentHandler = requireNonNull(beforeCommandSentHandler, "beforeCommandSentHandler");
    }

    @FunctionalInterface
    public interface BeforeCommandSent {
        void beforeCommandSent(Command command);
    }
}
