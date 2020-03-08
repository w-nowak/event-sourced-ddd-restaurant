package com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.mocking;

import com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.CommandChannelFactory;
import com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.CommandResponseChannel;
import com.wnowakcraft.samples.restaurant.core.domain.model.Command;
import com.wnowakcraft.samples.restaurant.core.domain.model.Response;
import com.wnowakcraft.samples.restaurant.core.utils.AsyncTestSupportSupport;
import com.wnowakcraft.samples.restaurant.core.utils.AsyncTestWaitSupport;
import lombok.RequiredArgsConstructor;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

import static lombok.AccessLevel.PRIVATE;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

@RequiredArgsConstructor(access = PRIVATE)
public class CommandResponseChannelMock implements WhenOnCommand {
    private final ArgumentCaptor<Consumer<Response>> commandResponseConsumerCaptor;
    private final Collection<Class<? extends Response>> asyncFlowFinishedResponseTypes;
    private final SentCommandNotifier sentCommandNotifierMock = mock(SentCommandNotifier.class);
    private AsyncCommandResponse asyncCommandResponse = new AsyncPoolCommandResponse();
    private AsyncTestSupportSupport asyncTestSupport = new AsyncTestSupportSupport();


    public static CommandResponseChannelMock mockCommandResponseChannel(
            String channelName, CommandChannelFactory commandChannelFactoryMock,
            Collection<Class<? extends Response>> asyncFlowFinishedResponseTypes){

        CommandResponseChannel commandResponseChannel = mock(CommandResponseChannel.class);

        ArgumentCaptor<Consumer<Response>> commandResponseConsumerCaptor = ArgumentCaptor.forClass(Consumer.class);
        willDoNothing().given(commandResponseChannel).invokeOnCommand(commandResponseConsumerCaptor.capture());

        given(commandChannelFactoryMock.createResponseChannel(channelName))
                .willReturn(CompletableFuture.completedFuture(commandResponseChannel));

        return new CommandResponseChannelMock(commandResponseConsumerCaptor, asyncFlowFinishedResponseTypes);
    }


    @SafeVarargs
    public static Collection<Class<? extends Response>> allowedFlowFinishedResponses(Class<? extends Response>... responses) {
        return List.of(responses);
    }

    @Override
    public CommandResponseChannelMock when(Class<? extends Command> commandTypeIssued, ThenRespondWith thenRespondWith) {
        BDDMockito.BDDStubber stubber = null;
        var commandResponseIterator = thenRespondWith.getCommandResponses().iterator();

        if(commandResponseIterator.hasNext()) {
            var commandResponse = commandResponseIterator.next();
            stubber = willAnswer(invocationOnMock -> delegateToAsyncCommandResponse(commandResponse));
        }

        while(commandResponseIterator.hasNext()) {
            var commandResponse = commandResponseIterator.next();
            stubber = stubber.willAnswer(invocationOnMock -> delegateToAsyncCommandResponse(commandResponse));
        }

        if(stubber != null) {
            stubber.given(sentCommandNotifierMock).notifyCommandSent(any(commandTypeIssued));
        }

        return this;
    }

    private Void delegateToAsyncCommandResponse(Response expectedResponse) {
        if(!asyncTestSupport.isAsyncFlowStarted()) {
            asyncTestSupport.startAsyncFlow();
        }

        asyncCommandResponse.scheduleResponse(
            commandResponseConsumerCaptor.getValue(),
            expectedResponse
        );
        return null;
    }

    public SentCommandNotifier getSentCommandNotifier() {
        return sentCommandNotifierMock;
    }

    public AsyncTestWaitSupport getAsyncTestWaitSupport() {
        return this.asyncTestSupport;
    }

    public void acceptNewCommandResponseJustReceived(Response commandResponse) {
        commandResponseConsumerCaptor.getValue().accept(commandResponse);
    }

    public interface AsyncCommandResponse {
        void scheduleResponse(Consumer<Response> responseConsumer, Response response);
    }

    private class AsyncPoolCommandResponse implements AsyncCommandResponse {

        @Override
        public void scheduleResponse(Consumer<Response> responseConsumer, Response actualResponse) {
            ForkJoinPool.commonPool().execute(() -> {
                responseConsumer.accept(actualResponse);
                if(asyncFlowFinishedResponseTypes.stream().anyMatch(responseType -> responseType.isAssignableFrom(actualResponse.getClass()))) {
                    asyncTestSupport.finishAsyncFlow();
                }
            }
            );
        }
    }

    @FunctionalInterface
    public interface SentCommandNotifier {
        void notifyCommandSent(Command command);
    }
}
