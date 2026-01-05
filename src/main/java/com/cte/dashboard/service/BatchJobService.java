package com.cte.dashboard.service;

import com.cte.dashboard.entity.BatchJobInfo;
import com.cte.dashboard.repository.BatchJobInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BatchJobService {

    private static final Logger log = LoggerFactory.getLogger(BatchJobService.class);

    private final JobLauncher jobLauncher;
    private final Job sampleJob;
    private final BatchJobInfoRepository batchJobInfoRepository;

    public BatchJobService(JobLauncher jobLauncher, Job sampleJob, BatchJobInfoRepository batchJobInfoRepository) {
        this.jobLauncher = jobLauncher;
        this.sampleJob = sampleJob;
        this.batchJobInfoRepository = batchJobInfoRepository;
    }

    public BatchJobInfo runSampleJob() {
        BatchJobInfo jobInfo = new BatchJobInfo("sampleJob", "STARTED", LocalDateTime.now());

        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(sampleJob, params);

            jobInfo.setStatus("COMPLETED");
            jobInfo.setEndTime(LocalDateTime.now());
            log.info("Sample job completed successfully");

        } catch (Exception e) {
            jobInfo.setStatus("FAILED");
            jobInfo.setEndTime(LocalDateTime.now());
            jobInfo.setErrorMessage(e.getMessage());
            log.error("Sample job failed", e);
        }

        return batchJobInfoRepository.save(jobInfo);
    }

    public List<BatchJobInfo> getRecentJobs() {
        return batchJobInfoRepository.findTop10ByOrderByCreatedAtDesc();
    }

    public List<BatchJobInfo> getJobsByStatus(String status) {
        return batchJobInfoRepository.findByStatusOrderByCreatedAtDesc(status);
    }
}
