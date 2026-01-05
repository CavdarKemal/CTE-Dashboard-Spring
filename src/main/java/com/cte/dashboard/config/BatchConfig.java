package com.cte.dashboard.config;

import com.cte.dashboard.batch.SampleItemProcessor;
import com.cte.dashboard.batch.SampleItemReader;
import com.cte.dashboard.batch.SampleItemWriter;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfig {

    @Bean
    public Job sampleJob(JobRepository jobRepository, Step sampleStep) {
        return new JobBuilder("sampleJob", jobRepository)
                .start(sampleStep)
                .build();
    }

    @Bean
    public Step sampleStep(JobRepository jobRepository,
                           PlatformTransactionManager transactionManager,
                           SampleItemReader reader,
                           SampleItemProcessor processor,
                           SampleItemWriter writer) {
        return new StepBuilder("sampleStep", jobRepository)
                .<String, String>chunk(10, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}
