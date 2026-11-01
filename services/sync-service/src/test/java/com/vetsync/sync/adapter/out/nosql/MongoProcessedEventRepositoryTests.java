package com.vetsync.sync.adapter.out.nosql;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import org.mockito.ArgumentCaptor;

class MongoProcessedEventRepositoryTests {

    private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    private final MongoProcessedEventRepository repository =
            new MongoProcessedEventRepository(mongoTemplate);

    @Test
    void checksForPreviouslyProcessedKeys() {
        when(mongoTemplate.exists(any(Query.class), eq(ProcessedEventDocument.class))).thenReturn(true);

        assertThat(repository.contains("SW:CLIENT:entity:4")).isTrue();
    }

    @Test
    void insertsProcessedMarkerWithoutOverwritingExistingMarker() {
        Instant processedAt = Instant.parse("2026-10-30T10:00:00Z");
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

        repository.markProcessed("SW:CLIENT:entity:4", processedAt);

        verify(mongoTemplate).upsert(
                queryCaptor.capture(),
                updateCaptor.capture(),
                eq(ProcessedEventDocument.class));
        assertThat(queryCaptor.getValue().getQueryObject()).containsEntry("_id", "SW:CLIENT:entity:4");
        Document setOnInsert = (Document) updateCaptor.getValue().getUpdateObject().get("$setOnInsert");
        assertThat(setOnInsert.get("processedAt")).isEqualTo(processedAt);
        assertThat(setOnInsert).containsOnlyKeys("processedAt");
    }
}
