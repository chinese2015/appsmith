package com.appsmith.server.x.configurations.mongo;

import com.appsmith.external.models.BaseDomain;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

@Slf4j
public class CustomReactiveMongoTemplate extends ReactiveMongoTemplate {

    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int SMALL_BATCH_THRESHOLD = 10;
    private final MongoProperties mongoProperties;

    public CustomReactiveMongoTemplate(
            ReactiveMongoDatabaseFactory mongoDbFactory,
            MongoConverter mongoConverter,
            MongoProperties mongoProperties) {
        super(mongoDbFactory, mongoConverter);
        this.mongoProperties = mongoProperties;
    }

    @Override
    public Mono<UpdateResult> updateMulti(Query query, UpdateDefinition update, Class<?> entityClass) {
        if (!isCosmosDB()) {
            return super.updateMulti(query, update, entityClass);
        }
        return processCosmosDBUpdate(query, update, entityClass);
    }

    @Override
    public Mono<UpdateResult> updateMulti(Query query, UpdateDefinition update, String collectionName) {
        if (!isCosmosDB()) {
            return super.updateMulti(query, update, collectionName);
        }
        Class<?> entityClass = getCollectionClass(collectionName);
        return processCosmosDBUpdate(query, update, entityClass);
    }

    private boolean isCosmosDB() {
        String uri = mongoProperties.getUri();
        if (uri == null) {
            return false;
        }
        return uri.contains(".mongo.cosmos.azure.com")
                || uri.contains("cosmos.azure.")
                || uri.contains("@cosmos.")
                || (uri.contains("ssl=true") && uri.contains("retrywrites=false"));
    }

    private Mono<UpdateResult> processCosmosDBUpdate(Query query, UpdateDefinition update, Class<?> entityClass) {
        return count(query, entityClass)
                .flatMap(total -> {
                    if (total == 0) {
                        return Mono.just(UpdateResult.acknowledged(0, 0L, null));
                    }

                    if (total <= SMALL_BATCH_THRESHOLD) {
                        return super.updateMulti(query, update, entityClass).onErrorResume(e -> {
                            log.debug(
                                    "Failed to use native update for small batch, falling back to batch processing", e);
                            return processBatchUpdate(query, update, entityClass, total);
                        });
                    }

                    return processBatchUpdate(query, update, entityClass, total);
                })
                .onErrorResume(e -> {
                    log.error("Error in CosmosDB update", e);
                    return Mono.error(e);
                });
    }

    private Mono<UpdateResult> processBatchUpdate(
            Query query, UpdateDefinition update, Class<?> entityClass, long total) {
        int batchSize = determineBatchSize(total);

        return Flux.range(0, (int) Math.ceil((double) total / batchSize))
                .flatMap(batch -> {
                    int skip = batch * batchSize;
                    Query batchQuery = Query.of(query).skip(skip).limit(batchSize);
                    return find(batchQuery, entityClass)
                            .collectList()
                            .flatMap(docs -> processSingleBatch(docs, update, entityClass));
                })
                .reduce((result1, result2) -> UpdateResult.acknowledged(
                        result1.getMatchedCount() + result2.getMatchedCount(),
                        result1.getModifiedCount() + result2.getModifiedCount(),
                        null));
    }

    private int determineBatchSize(long total) {
        if (total < 100) return 10;
        if (total < 1000) return 50;
        return DEFAULT_BATCH_SIZE;
    }

    private Mono<UpdateResult> processSingleBatch(List<?> batch, UpdateDefinition update, Class<?> entityClass) {
        return Flux.fromIterable(batch)
                .flatMap(doc -> {
                    String id = getDocumentId(doc);
                    if (id == null) {
                        return Mono.empty();
                    }

                    return updateFirst(Query.query(Criteria.where("_id").is(id)), update, entityClass)
                            .onErrorResume(e -> {
                                log.error("Error updating document {}: {}", id, e.getMessage());
                                return Mono.empty();
                            });
                })
                .reduce((result1, result2) -> UpdateResult.acknowledged(
                        result1.getMatchedCount() + result2.getMatchedCount(),
                        result1.getModifiedCount() + result2.getModifiedCount(),
                        null))
                .defaultIfEmpty(UpdateResult.acknowledged(0, 0L, null));
    }

    private String getDocumentId(Object document) {
        if (document instanceof BaseDomain) {
            return ((BaseDomain) document).getId();
        }
        return null;
    }

    private Class<?> getCollectionClass(String collectionName) {
        MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext =
                getConverter().getMappingContext();

        if (mappingContext instanceof MongoMappingContext) {
            MongoMappingContext mongoMappingContext = (MongoMappingContext) mappingContext;
            Collection<? extends MongoPersistentEntity<?>> entities = mongoMappingContext.getPersistentEntities();

            for (MongoPersistentEntity<?> entity : entities) {
                if (collectionName.equals(entity.getCollection())) {
                    return entity.getType();
                }
            }
        }

        log.warn("No entity mapping found for collection: {}", collectionName);
        return null;
    }
}
