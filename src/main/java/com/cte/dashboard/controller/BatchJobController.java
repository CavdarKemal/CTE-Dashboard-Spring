package com.cte.dashboard.controller;

import com.cte.dashboard.entity.BatchJobInfo;
import com.cte.dashboard.service.BatchJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batch")
public class BatchJobController {

    private final BatchJobService batchJobService;

    public BatchJobController(BatchJobService batchJobService) {
        this.batchJobService = batchJobService;
    }

    @PostMapping("/run/sample")
    public ResponseEntity<BatchJobInfo> runSampleJob() {
        BatchJobInfo result = batchJobService.runSampleJob();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<BatchJobInfo>> getRecentJobs() {
        return ResponseEntity.ok(batchJobService.getRecentJobs());
    }

    @GetMapping("/jobs/status/{status}")
    public ResponseEntity<List<BatchJobInfo>> getJobsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(batchJobService.getJobsByStatus(status));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "CTE-Dashboard Batch Service"
        ));
    }
}
