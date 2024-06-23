package com.example.demo;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/document")
public class DocumentController {

    private final BatchJobService batchJobService;

    public DocumentController(BatchJobService batchJobService) {
        this.batchJobService = batchJobService;
    }

    @PostMapping("/batch")
    public String submitBatchJob(@RequestBody BatchRequest request) {
        Job job = new Job(request.getNumberOfDocuments(), request.getNeedsApproval());
        batchJobService.submitFullJob(job);
        return "Batch job submitted";
    }

    @GetMapping("/results")
    public String getResults() {
        StringBuilder results = new StringBuilder();
        batchJobService.getJobs().forEach(job -> {
            results.append("Job ID: ").append(job.getId()).append("\n");
            results.append("Status: ").append(job.getStatus()).append("\n");
            results.append("Finished documents: ").append(job.getFinishedDocuments().toString()).append("\n\n");
        });
        return results.toString();
    }

    @PostMapping("/archive/approval")
    public String archivingApproval(@RequestBody ApprovalRequestDTO approvalRequestDTO) {
        batchJobService.getJobs().stream()
                .filter(job -> job.getId().equals(approvalRequestDTO.getJobId()))
                .findFirst()
                .ifPresent(job -> batchJobService.submitArchiveJob(job, approvalRequestDTO.getApproved()));
        return "Archiving job submitted";
    }

}
