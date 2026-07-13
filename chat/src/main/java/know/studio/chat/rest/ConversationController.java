package know.studio.chat.rest;

import jakarta.validation.Valid;
import know.studio.chat.api.ConversationApi;
import know.studio.chat.api.ConversationContext;
import know.studio.chat.api.CreateSessionCommand;
import know.studio.chat.api.SessionInfo;
import know.studio.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationApi conversationApi;

    @GetMapping
    public ApiResponse<java.util.List<SessionInfo>> list() {
        return ApiResponse.ok(conversationApi.listSessions());
    }

    @PostMapping
    public ApiResponse<SessionInfo> create(
            @Valid @RequestBody CreateSessionRequest request
    ) {
        return ApiResponse.ok(conversationApi.createSession(new CreateSessionCommand(
                request.title(),
                request.toolMode(),
                request.deepThinking()
        )));
    }

    @GetMapping("/{sessionId}/context")
    public ApiResponse<ConversationContext> context(
            @PathVariable long sessionId,
            @RequestParam(defaultValue = "") String question
    ) {
        return ApiResponse.ok(conversationApi.loadContext(sessionId, question));
    }

    @PatchMapping("/{sessionId}")
    public ApiResponse<SessionInfo> rename(
            @PathVariable long sessionId,
            @Valid @RequestBody RenameSessionRequest request
    ) {
        return ApiResponse.ok(conversationApi.renameSession(sessionId, request.title()));
    }

    @DeleteMapping("/{sessionId}")
    public ApiResponse<Void> delete(
            @PathVariable long sessionId
    ) {
        conversationApi.deleteSession(sessionId);
        return ApiResponse.ok();
    }
}
