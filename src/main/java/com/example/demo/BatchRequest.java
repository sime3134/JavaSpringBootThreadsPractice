package com.example.demo;

public class BatchRequest {
    private int numberOfDocuments;
    private boolean needsApproval;

    public int getNumberOfDocuments() {
        return numberOfDocuments;
    }

    public void setNumberOfDocuments(Integer numberOfDocuments) {
        this.numberOfDocuments = numberOfDocuments;
    }

    public boolean getNeedsApproval() {
        return needsApproval;
    }

    public void setNeedsApproval(Boolean needsApproval) {
        this.needsApproval = needsApproval;
    }
}
