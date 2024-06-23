package com.example.demo;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

@Service
public class BatchJobService {

    private final WebClient webClient;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final List<Job> jobs = new CopyOnWriteArrayList<>();

    public BatchJobService(WebClient.Builder webClientBuilder,
                           @Qualifier("batchJobExecutor") Executor taskExecutor) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8081").build();
        this.taskExecutor = (ThreadPoolTaskExecutor) taskExecutor;
    }

    public void submitFullJob(Job job) {
        job.setStatus(JobStatus.IN_PROGRESS);
        taskExecutor.execute(() -> processJob(job));
    }

    public void submitArchiveJob(Job job, boolean approved) {
        if(approved) {
            job.setStatus(JobStatus.ARCHIVING);
            taskExecutor.execute(() -> archiveJob(job));
        } else {
            job.setStatus(JobStatus.REJECTED);
            System.out.println("Job rejected: " + job.getId());
        }
    }

    private void archiveJob(Job job) {
        System.out.println("Archiving job: " + job.getId());
        System.out.println("Number of documents: " + job.getNumberOfDocuments());

        Flux.fromIterable(job.getDocuments())
                .flatMap(document -> archiveDocument(document, job), 20)
                .doOnError(throwable -> {
                    job.setStatus(JobStatus.FAILED);
                    System.out.println("Job failed: " + job.getId());
                })
                .doOnComplete(() -> {
                    job.setStatus(JobStatus.COMPLETED);
                    System.out.println("Job completed: " + job.getId() + " with " + job.getDocuments().size() +
                            " documents");
                })
                .subscribe();
    }

    private void processJob(Job job) {
        System.out.println("Processing job: " + job.getId());
        System.out.println("Number of documents: " + job.getNumberOfDocuments());

        Flux.range(0, job.getNumberOfDocuments())
                .flatMap(documentIndex -> getMetadata(documentIndex, job)
                        .flatMap(metadata -> generateDocument(metadata, job))
                        .flatMap(document -> uploadToCloud(document, job))
                                .flatMap(document -> {
                                    if(job.needsApproval()) {
                                        job.addDocument(document);
                                        return Mono.empty();
                                    } else {
                                        return archiveDocument(document, job);
                                    }
                                })
                        , 10)
                .doOnError(throwable -> {
                    job.setStatus(JobStatus.FAILED);
                    System.out.println("Archiving failed: " + job.getId());
                })
                .doOnComplete(() -> {
                    if(job.needsApproval()) {
                        System.out.println("Job pending approval: " + job.getId());
                        job.setStatus(JobStatus.PENDING_APPROVAL);
                        seekApproval(job);
                    } else {
                        job.setStatus(JobStatus.COMPLETED);
                        System.out.println("Job completed: " + job.getId());
                    }
                    jobs.add(job);
                })
                .subscribe();
    }

    private Mono<String> getMetadata(Integer documentIndex, Job job) {
        return webClient.post()
                .uri("/api/v1/metadata")
                .bodyValue(documentIndex)
                .retrieve()
                .bodyToMono(String.class)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(throwable -> {
                    throwable.printStackTrace();
                });
    }

    private Mono<String> generateDocument(String documentName, Job job) {
        return webClient.post()
                .uri("/api/v1/document")
                .bodyValue(documentName)
                .retrieve()
                .bodyToMono(String.class)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(throwable -> {
                    throwable.printStackTrace();
                });

    }

    private Mono<String> uploadToCloud(String document, Job job) {
        return webClient.post()
                .uri("/api/v1/cloud")
                .bodyValue(document)
                .retrieve()
                .bodyToMono(String.class)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(throwable -> {
                    throwable.printStackTrace();
                });
    }

    private void seekApproval(Job job) {
        webClient.post()
                .uri("/api/v1/approval")
                .bodyValue(job.getId())
                .retrieve()
                .toEntity(String.class)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(response -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        return Mono.just("Approval request sent");
                    } else {
                        return Mono.error(new RuntimeException("Approval request failed"));
                    }
                })
                .doOnError(throwable -> {
                    throwable.printStackTrace();
                }).subscribe();
    }

    private Mono<String> archiveDocument(String document, Job job) {
        return webClient.post()
                .uri("/api/v1/archive")
                .bodyValue(document)
                .retrieve()
                .bodyToMono(String.class)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(response -> {
                    job.incrementFinishedDocuments();
                })
                .doOnError(throwable -> {
                    throwable.printStackTrace();
                });
    }

    public List<Job> getJobs() {
        return jobs;
    }
}
