package com.example.demo;

import ch.qos.logback.core.joran.event.BodyEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class Job {
    private final Long id;
    private JobStatus status;
    private final int numberOfDocuments;
    private final boolean needsApproval;
    private final AtomicInteger finishedDocuments = new AtomicInteger(0);

    private final List<String> documents;

    public Job(int numberOfDocuments, boolean needsApproval) {
        this.id = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        this.status = JobStatus.PENDING;
        this.numberOfDocuments = numberOfDocuments;
        this.documents = new ArrayList<>();
        this.needsApproval = needsApproval;
    }

    public Long getId() {
        return id;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public List<String> getDocuments() {
        return documents;
    }

    public synchronized void addDocument(String document) {
        documents.add(document);
    }

    public int getNumberOfDocuments() {
        return numberOfDocuments;
    }

    public boolean needsApproval() {
        return needsApproval;
    }

    public void incrementFinishedDocuments() {
        finishedDocuments.incrementAndGet();
    }

    public Integer getFinishedDocuments() {
        return finishedDocuments.get();
    }

}
