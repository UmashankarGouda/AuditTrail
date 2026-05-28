package com.audittrail.batch;

import com.audittrail.entity.MetadataChange;
import com.audittrail.repository.MetadataChangeRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;
import jakarta.persistence.EntityManagerFactory;

@Configuration
public class BatchConfig {

    @Autowired
    private MetadataChangeRepository metadataChangeRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Bean
    public FlatFileItemReader<MetadataChangeCsvRecord> csvReader() {
        return new FlatFileItemReaderBuilder<MetadataChangeCsvRecord>()
                .name("csvReader")
                .linesToSkip(1) // Skip header
                .delimited()
                .delimiter(",")
                .names("componentName", "componentType", "changeType", "changedBy", "deploymentId", "changedAt", "oldValue", "newValue")
                .targetType(MetadataChangeCsvRecord.class)
                .build();
    }

    @Bean
    public MetadataChangeItemProcessor processor() {
        return new MetadataChangeItemProcessor();
    }

    @Bean
    public JpaItemWriter<MetadataChange> writer() {
        return new JpaItemWriterBuilder<MetadataChange>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    @Bean
    public Step importStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                          FlatFileItemReader<MetadataChangeCsvRecord> reader,
                          MetadataChangeItemProcessor processor,
                          JpaItemWriter<MetadataChange> writer) {
        return new StepBuilder("importStep", jobRepository)
                .<MetadataChangeCsvRecord, MetadataChange>chunk(500, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .transactionManager(transactionManager)
                .faultTolerant()
                .skipLimit(100) // Skip up to 100 bad records
                .skip(Exception.class)
                .build();
    }

    @Bean
    public Job importMetadataJob(JobRepository jobRepository, Step importStep) {
        return new JobBuilder("importMetadataJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(importStep)
                .build();
    }
}
