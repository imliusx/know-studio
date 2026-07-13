package know.studio.agent.rest;

import jakarta.validation.Valid;
import know.studio.agent.api.AgentApi;
import know.studio.agent.api.ChatRequest;
import know.studio.common.sse.SseEmitterSender;
import know.studio.common.ratelimit.RateLimit;
import know.studio.common.trace.RagTraceNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private static final long SSE_TIMEOUT_MILLIS = Duration.ofMinutes(5).toMillis();

    private final AgentApi agentApi;

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(key = "agent.chat", permits = 5, windowSeconds = 1)
    @RagTraceNode("api.agent.stream")
    public SseEmitter streamChat(
            @Valid @RequestBody StreamChatRequest request
    ) {
        SseEmitterSender sender = new SseEmitterSender(SSE_TIMEOUT_MILLIS);
        agentApi.streamChat(new ChatRequest(
                        request.sessionId(),
                        request.message(),
                        request.knowledgeBaseIds(),
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
