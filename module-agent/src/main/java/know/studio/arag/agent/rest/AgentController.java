package know.studio.arag.agent.rest;

import jakarta.validation.Valid;
import know.studio.arag.agent.api.AgentApi;
import know.studio.arag.agent.api.ChatRequest;
import know.studio.arag.platform.core.sse.SseEmitterSender;
import know.studio.arag.platform.core.ratelimit.RateLimit;
import know.studio.arag.platform.core.trace.RagTraceNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/agent")
@RequiredArgsConstructor
public class AgentController {

    private static final long SSE_TIMEOUT_MILLIS = Duration.ofMinutes(5).toMillis();

    private final AgentApi agentApi;

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(key = "agent.chat", permits = 5, windowSeconds = 1)
    @RagTraceNode("api.agent.stream")
    public SseEmitter streamChat(
            @PathVariable long workspaceId,
            @Valid @RequestBody StreamChatRequest request
    ) {
        SseEmitterSender sender = new SseEmitterSender(SSE_TIMEOUT_MILLIS);
        agentApi.streamChat(new ChatRequest(
                        request.sessionId(),
                        workspaceId,
                        request.message(),
                        request.toolMode(),
                        request.deepThinking()
                ))
                .subscribe(
                        event -> sender.send(event.type().eventName(), event.payload()),
                        sender::completeWithError,
                        sender::complete
                );
        return sender.emitter();
    }
}
