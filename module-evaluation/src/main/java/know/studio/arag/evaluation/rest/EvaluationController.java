package know.studio.arag.evaluation.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import know.studio.arag.evaluation.api.AddSampleCommand;
import know.studio.arag.evaluation.api.CreateDatasetCommand;
import know.studio.arag.evaluation.api.DatasetInfo;
import know.studio.arag.evaluation.api.EvaluationApi;
import know.studio.arag.evaluation.api.EvaluationReport;
import know.studio.arag.evaluation.api.EvaluationRunInfo;
import know.studio.arag.evaluation.api.SampleInfo;
import know.studio.arag.platform.core.ratelimit.RateLimit;
import know.studio.arag.platform.core.response.ApiResponse;
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
@RequestMapping("/api/workspaces/{workspaceId}/evaluations")
@RequiredArgsConstructor
@Validated
public class EvaluationController {

    private final EvaluationApi evaluationApi;

    @GetMapping("/datasets")
    public ApiResponse<List<DatasetInfo>> listDatasets(@PathVariable long workspaceId) {
        return ApiResponse.ok(evaluationApi.listDatasets(workspaceId));
    }

    @PostMapping("/datasets")
    public ApiResponse<DatasetInfo> createDataset(
            @PathVariable long workspaceId,
            @Valid @RequestBody CreateDatasetRequest request
    ) {
        return ApiResponse.ok(evaluationApi.createDataset(new CreateDatasetCommand(
                workspaceId,
                request.name(),
                request.description()
        )));
    }

    @PostMapping("/datasets/{datasetId}/samples")
    public ApiResponse<SampleInfo> addSample(
            @PathVariable long workspaceId,
            @PathVariable long datasetId,
            @Valid @RequestBody AddSampleRequest request
    ) {
        return ApiResponse.ok(evaluationApi.addSample(new AddSampleCommand(
                workspaceId,
                datasetId,
                request.question(),
                request.relevantChunkIds(),
                request.expectedAnswer()
        )));
    }

    @GetMapping("/datasets/{datasetId}/samples")
    public ApiResponse<List<SampleInfo>> listSamples(
            @PathVariable long workspaceId,
            @PathVariable long datasetId
    ) {
        return ApiResponse.ok(evaluationApi.listSamples(workspaceId, datasetId));
    }

    @GetMapping("/datasets/{datasetId}/runs")
    public ApiResponse<List<EvaluationRunInfo>> listRuns(
            @PathVariable long workspaceId,
            @PathVariable long datasetId
    ) {
        return ApiResponse.ok(evaluationApi.listRuns(workspaceId, datasetId));
    }

    @PostMapping("/datasets/{datasetId}/runs/ablation")
    @RateLimit(key = "evaluation.ablation", permits = 1, windowSeconds = 10)
    public ApiResponse<EvaluationReport> runAblation(
            @PathVariable long workspaceId,
            @PathVariable long datasetId,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int topK
    ) {
        return ApiResponse.ok(evaluationApi.runAblation(workspaceId, datasetId, topK));
    }
}
