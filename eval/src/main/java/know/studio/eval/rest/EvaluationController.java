package know.studio.eval.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import know.studio.eval.api.AddSampleCommand;
import know.studio.eval.api.CreateDatasetCommand;
import know.studio.eval.api.DatasetInfo;
import know.studio.eval.api.EvaluationApi;
import know.studio.eval.api.EvaluationReport;
import know.studio.eval.api.EvaluationRunInfo;
import know.studio.eval.api.SampleInfo;
import know.studio.common.ratelimit.RateLimit;
import know.studio.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases/{knowledgeBaseId}/evaluations")
@RequiredArgsConstructor
@Validated
public class EvaluationController {

    private final EvaluationApi evaluationApi;

    @GetMapping("/datasets")
    public ApiResponse<List<DatasetInfo>> listDatasets(@PathVariable long knowledgeBaseId) {
        return ApiResponse.ok(evaluationApi.listDatasets(knowledgeBaseId));
    }

    @PostMapping("/datasets")
    public ApiResponse<DatasetInfo> createDataset(
            @PathVariable long knowledgeBaseId,
            @Valid @RequestBody CreateDatasetRequest request
    ) {
        return ApiResponse.ok(evaluationApi.createDataset(new CreateDatasetCommand(
                knowledgeBaseId,
                request.name(),
                request.description()
        )));
    }

    @PostMapping("/datasets/{datasetId}/samples")
    public ApiResponse<SampleInfo> addSample(
            @PathVariable long knowledgeBaseId,
            @PathVariable long datasetId,
            @Valid @RequestBody AddSampleRequest request
    ) {
        return ApiResponse.ok(evaluationApi.addSample(new AddSampleCommand(
                knowledgeBaseId,
                datasetId,
                request.question(),
                request.relevantChunkIds(),
                request.expectedAnswer(),
                request.expectRefusal()
        )));
    }

    @GetMapping("/datasets/{datasetId}/samples")
    public ApiResponse<List<SampleInfo>> listSamples(
            @PathVariable long knowledgeBaseId,
            @PathVariable long datasetId
    ) {
        return ApiResponse.ok(evaluationApi.listSamples(knowledgeBaseId, datasetId));
    }

    @GetMapping("/datasets/{datasetId}/runs")
    public ApiResponse<List<EvaluationRunInfo>> listRuns(
            @PathVariable long knowledgeBaseId,
            @PathVariable long datasetId
    ) {
        return ApiResponse.ok(evaluationApi.listRuns(knowledgeBaseId, datasetId));
    }

    @PostMapping("/datasets/{datasetId}/runs/ablation")
    @RateLimit(key = "evaluation.ablation", permits = 1, windowSeconds = 10)
    public ApiResponse<EvaluationReport> runAblation(
            @PathVariable long knowledgeBaseId,
            @PathVariable long datasetId,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int topK
    ) {
        return ApiResponse.ok(evaluationApi.runAblation(knowledgeBaseId, datasetId, topK));
    }
}
