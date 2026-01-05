package com.cte.dashboard.batch;

import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Component
public class SampleItemReader implements ItemReader<String> {

    private final Iterator<String> dataIterator;

    public SampleItemReader() {
        List<String> sampleData = Arrays.asList(
                "Item 1", "Item 2", "Item 3", "Item 4", "Item 5"
        );
        this.dataIterator = sampleData.iterator();
    }

    @Override
    public String read() {
        if (dataIterator.hasNext()) {
            return dataIterator.next();
        }
        return null;
    }
}
