package com.cte.dashboard.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class SampleItemWriter implements ItemWriter<String> {

    private static final Logger log = LoggerFactory.getLogger(SampleItemWriter.class);

    @Override
    public void write(Chunk<? extends String> items) {
        for (String item : items) {
            log.info("Writing item: {}", item);
        }
    }
}
