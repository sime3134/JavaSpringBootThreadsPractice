package com.example.demo;

public class ApprovalRequestDTO {
    private Long jobId;
    private boolean approved;

    public ApprovalRequestDTO() {
    }

    public ApprovalRequestDTO(Long jobId, boolean approved) {
        this.jobId = jobId;
        this.approved = approved;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }
}
