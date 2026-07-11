package know.studio.arag.identity.rest;

import jakarta.validation.Valid;
import know.studio.arag.identity.api.WorkspaceInfo;
import know.studio.arag.identity.api.WorkspaceMemberInfo;
import know.studio.arag.identity.domain.IdentityService;
import know.studio.arag.platform.core.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final IdentityService identityService;

    @GetMapping
    public ApiResponse<List<WorkspaceInfo>> list() {
        return ApiResponse.ok(identityService.listWorkspaces());
    }

    @PostMapping
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody CreateWorkspaceRequest request) {
        long workspaceId = identityService.createWorkspace(request.name(), request.description());
        return ApiResponse.ok(Map.of("workspaceId", workspaceId));
    }

    @PostMapping("/{workspaceId}/members")
    public ApiResponse<Void> addMember(
            @PathVariable long workspaceId,
            @Valid @RequestBody AddWorkspaceMemberRequest request
    ) {
        identityService.addMember(workspaceId, request.email(), request.role());
        return ApiResponse.ok(null);
    }

    @GetMapping("/{workspaceId}/members")
    public ApiResponse<List<WorkspaceMemberInfo>> listMembers(@PathVariable long workspaceId) {
        return ApiResponse.ok(identityService.listMembers(workspaceId));
    }
}
