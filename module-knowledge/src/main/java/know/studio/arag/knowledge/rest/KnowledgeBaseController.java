package know.studio.arag.knowledge.rest;

import jakarta.validation.Valid;
import know.studio.arag.knowledge.api.KnowledgeBaseInfo;
import know.studio.arag.knowledge.domain.KnowledgeBaseService;
import know.studio.arag.platform.core.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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

    @PostMapping
    public ApiResponse<Map<String, Long>> create(
            @Valid @RequestBody CreateKnowledgeBaseRequest request
    ) {
        long knowledgeBaseId = knowledgeBaseService.create(
                request.name(),
                request.description(),
                request.visibility(),
                request.ownerTeamId()
        );
        return ApiResponse.ok(Map.of("knowledgeBaseId", knowledgeBaseId));
    }
}
