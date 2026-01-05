package com.cte.dashboard.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class SampleItemProcessor implements ItemProcessor<String, String> {

    private static final Logger log = LoggerFactory.getLogger(SampleItemProcessor.class);

    @Override
    public String process(String item) {
        String processedItem = item.toUpperCase();
        log.info("Processing: {} -> {}", item, processedItem);
        return processedItem;
    }
}
