package com.wnowakcraft.samples.restaurant.core.infrastructure.messaging;

import com.wnowakcraft.samples.restaurant.core.domain.model.Command;
import com.wnowakcraft.samples.restaurant.core.domain.model.Response;
import com.wnowakcraft.samples.restaurant.core.utils.AsyncTestSupportSupport;
import com.wnowakcraft.samples.restaurant.core.utils.AsyncTestWaitSupport;
import lombok.RequiredArgsConstructor;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

import static lombok.AccessLevel.PRIVATE;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

@RequiredArgsConstructor(access = PRIVATE)
public class CommandResponseChannelMock {
    private final ArgumentCaptor<Consumer<Response>> commandResponseConsumerCaptor;
    private final Class<? extends Response> asyncFlowFinishedResponseClass;
    private final SentCommandNotifier sentCommandNotifierMock = mock(SentCommandNotifier.class);
    private AsyncCommandResponse asyncCommandResponse = new AsyncPoolCommandResponse();
    private AsyncTestSupportSupport asyncTestSupport = new AsyncTestSupportSupport();


    public static CommandResponseChannelMock mockCommandResponseChannel(
            String channelName, CommandChannelFactory commandChannelFactoryMock,
            Class<? extends Response> asyncFlowFinishedResponseClass){

        CommandResponseChannel commandResponseChannel = mock(CommandResponseChannel.class);

        ArgumentCaptor<Consumer<Response>> commandResponseConsumerCaptor = ArgumentCaptor.forClass(Consumer.class);
        willDoNothing().given(commandResponseChannel).invokeOnCommand(commandResponseConsumerCaptor.capture());

        given(commandChannelFactoryMock.createResponseChannel(channelName))
                .willReturn(CompletableFuture.completedFuture(commandResponseChannel));

        return new CommandResponseChannelMock(commandResponseConsumerCaptor, asyncFlowFinishedResponseClass);
    }

    public CommandResponseChannelMock when(Command commandIssued, ThenRespondWith thenRespondWith) {
        willAnswer(invocationOnMock -> delegateToAsyncCommandResponse(thenRespondWith.commandResponse))
                .given(sentCommandNotifierMock).notifyCommandSent(commandIssued);

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

    @RequiredArgsConstructor(access = PRIVATE)
    public static class ThenRespondWith {
        private final Response commandResponse;

        public static ThenRespondWith thenRespondWith(Response commandResponse) {
            return new ThenRespondWith(commandResponse);
        }
    }

    public interface AsyncCommandResponse {
        void scheduleResponse(Consumer<Response> responseConsumer, Response response);
    }

    private class AsyncPoolCommandResponse implements AsyncCommandResponse {

        @Override
        public void scheduleResponse(Consumer<Response> responseConsumer, Response response) {
            ForkJoinPool.commonPool().execute(() -> {
                responseConsumer.accept(response);
                if(response.getClass() == asyncFlowFinishedResponseClass) {
                    asyncTestSupport.finishAsyncFlow();
                }
            }
            );
        }
    }

    public interface SentCommandNotifier {
        void notifyCommandSent(Command command);
    }
}
