package know.studio.arag.conversation.rest;

import jakarta.validation.Valid;
import know.studio.arag.conversation.api.ConversationApi;
import know.studio.arag.conversation.api.ConversationContext;
import know.studio.arag.conversation.api.CreateSessionCommand;
import know.studio.arag.conversation.api.SessionInfo;
import know.studio.arag.platform.core.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/sessions")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationApi conversationApi;

    @PostMapping
    public ApiResponse<SessionInfo> create(
            @PathVariable long workspaceId,
            @Valid @RequestBody CreateSessionRequest request
    ) {
        return ApiResponse.ok(conversationApi.createSession(new CreateSessionCommand(
                workspaceId,
                request.title(),
                request.toolMode(),
                request.deepThinking()
        )));
    }

    @GetMapping("/{sessionId}/context")
    public ApiResponse<ConversationContext> context(
            @PathVariable long workspaceId,
            @PathVariable long sessionId,
            @RequestParam(defaultValue = "") String question
    ) {
        return ApiResponse.ok(conversationApi.loadContext(workspaceId, sessionId, question));
    }
}
