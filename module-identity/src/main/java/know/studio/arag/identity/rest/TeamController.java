package know.studio.arag.identity.rest;

import jakarta.validation.Valid;
import know.studio.arag.identity.api.TeamInfo;
import know.studio.arag.identity.api.TeamMemberInfo;
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
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final IdentityService identityService;

    @GetMapping
    public ApiResponse<List<TeamInfo>> list() {
        return ApiResponse.ok(identityService.listTeams());
    }

    @PostMapping
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody CreateTeamRequest request) {
        long teamId = identityService.createTeam(request.name(), request.description(), request.parentId());
        return ApiResponse.ok(Map.of("teamId", teamId));
    }

    @GetMapping("/{teamId}/members")
    public ApiResponse<List<TeamMemberInfo>> listMembers(@PathVariable long teamId) {
        return ApiResponse.ok(identityService.listTeamMembers(teamId));
    }

    @PostMapping("/{teamId}/members")
    public ApiResponse<Void> addMember(
            @PathVariable long teamId,
            @Valid @RequestBody AddTeamMemberRequest request
    ) {
        identityService.addTeamMember(teamId, request.email(), request.role());
        return ApiResponse.ok(null);
    }
}
