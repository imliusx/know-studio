package know.studio.arag.retrieval.rest;

import jakarta.validation.Valid;
import know.studio.arag.platform.core.response.ApiResponse;
import know.studio.arag.retrieval.api.EvidenceBundle;
import know.studio.arag.retrieval.api.RetrievalApi;
import know.studio.arag.retrieval.api.RetrievalQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/retrieval")
@RequiredArgsConstructor
public class RetrievalController {

    private static final int DEFAULT_TOP_K = 5;

    private final RetrievalApi retrievalApi;

    @PostMapping("/search")
    public ApiResponse<EvidenceBundle> search(
            @PathVariable long workspaceId,
            @Valid @RequestBody RetrievalRequest request
    ) {
        int topK = request.topK() == null ? DEFAULT_TOP_K : request.topK();
        return ApiResponse.ok(retrievalApi.retrieve(new RetrievalQuery(request.question(), workspaceId, topK)));
    }
}
