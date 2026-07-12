package know.studio.arag.retrieval.rest;

import jakarta.validation.Valid;
import know.studio.arag.platform.core.response.ApiResponse;
import know.studio.arag.platform.core.ratelimit.RateLimit;
import know.studio.arag.platform.core.trace.RagTraceNode;
import know.studio.arag.retrieval.api.EvidenceBundle;
import know.studio.arag.retrieval.api.RetrievalApi;
import know.studio.arag.retrieval.api.RetrievalQuery;
import know.studio.arag.retrieval.api.RetrievalMode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/retrieval")
@RequiredArgsConstructor
public class RetrievalController {

    private static final int DEFAULT_TOP_K = 5;

    private final RetrievalApi retrievalApi;

    @PostMapping("/search")
    @RateLimit(key = "retrieval.search", permits = 20, windowSeconds = 1)
    @RagTraceNode("api.retrieval.search")
    public ApiResponse<EvidenceBundle> search(
            @Valid @RequestBody RetrievalRequest request
    ) {
        int topK = request.topK() == null ? DEFAULT_TOP_K : request.topK();
        RetrievalMode mode = request.mode() == null ? RetrievalMode.HYBRID_RERANK : request.mode();
        return ApiResponse.ok(retrievalApi.retrieve(new RetrievalQuery(
                request.question(),
                request.knowledgeBaseIds(),
                topK,
                mode
        )));
    }
}
