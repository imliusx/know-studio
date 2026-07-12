package know.studio.arag.knowledge.rest;

import jakarta.validation.Valid;
import know.studio.arag.knowledge.api.KnowledgeBaseInfo;
import know.studio.arag.knowledge.api.KnowledgeBaseTeamGrantInfo;
import know.studio.arag.knowledge.domain.KnowledgeBaseService;
import know.studio.arag.platform.core.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @GetMapping
    public ApiResponse<List<KnowledgeBaseInfo>> list() {
        return ApiResponse.ok(knowledgeBaseService.listReadable());
    }

    @GetMapping("/{knowledgeBaseId}")
    public ApiResponse<KnowledgeBaseInfo> get(@PathVariable long knowledgeBaseId) {
        return ApiResponse.ok(knowledgeBaseService.get(knowledgeBaseId));
    }

    @PostMapping
    public ApiResponse<Map<String, String>> create(
            @Valid @RequestBody CreateKnowledgeBaseRequest request
    ) {
        long knowledgeBaseId = knowledgeBaseService.create(
                request.name(),
                request.description(),
                request.visibility(),
                request.ownerTeamId()
        );
        return ApiResponse.ok(Map.of("knowledgeBaseId", Long.toString(knowledgeBaseId)));
    }

    @PatchMapping("/{knowledgeBaseId}")
    public ApiResponse<Void> update(
            @PathVariable long knowledgeBaseId,
            @Valid @RequestBody UpdateKnowledgeBaseRequest request
    ) {
        knowledgeBaseService.update(
                knowledgeBaseId,
                request.name(),
                request.description(),
                request.visibility(),
                request.ownerTeamId()
        );
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{knowledgeBaseId}")
    public ApiResponse<Void> delete(@PathVariable long knowledgeBaseId) {
        knowledgeBaseService.delete(knowledgeBaseId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{knowledgeBaseId}/teams")
    public ApiResponse<List<KnowledgeBaseTeamGrantInfo>> listTeamGrants(
            @PathVariable long knowledgeBaseId
    ) {
        return ApiResponse.ok(knowledgeBaseService.listTeamGrants(knowledgeBaseId));
    }

    @PutMapping("/{knowledgeBaseId}/teams/{teamId}")
    public ApiResponse<Void> saveTeamGrant(
            @PathVariable long knowledgeBaseId,
            @PathVariable long teamId,
            @Valid @RequestBody UpdateKnowledgeBaseGrantRequest request
    ) {
        knowledgeBaseService.saveTeamGrant(knowledgeBaseId, teamId, request.permission());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{knowledgeBaseId}/teams/{teamId}")
    public ApiResponse<Void> deleteTeamGrant(
            @PathVariable long knowledgeBaseId,
            @PathVariable long teamId
    ) {
        knowledgeBaseService.deleteTeamGrant(knowledgeBaseId, teamId);
        return ApiResponse.ok(null);
    }
}
