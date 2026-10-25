package com.taskmanager.task;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class DatabaseVersionRepository {

    private final DatabaseClient client;

    public DatabaseVersionRepository(DatabaseClient client) {
        this.client = client;
    }

    public Mono<String> version() {
        return client.sql("SELECT version()")
                .map(row -> row.get(0, String.class))
                .one();
    }
}
