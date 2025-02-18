package com.appsmith.server.x.configurations.mongo;

import com.appsmith.external.models.BaseDomain;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

import java.util.Collection;
import java.util.List;

@Slf4j
public class CustomMongoTemplate extends MongoTemplate {

    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int SMALL_BATCH_THRESHOLD = 10;
    private final MongoProperties mongoProperties;

    public CustomMongoTemplate(
            MongoDatabaseFactory mongoDbFactory, MongoConverter mongoConverter, MongoProperties mongoProperties) {
        super(mongoDbFactory, mongoConverter);
        this.mongoProperties = mongoProperties;
    }

    @Override
    public UpdateResult updateMulti(Query query, UpdateDefinition update, Class<?> entityClass) {
        if (!isCosmosDB()) {
            return super.updateMulti(query, update, entityClass);
        }
        String collectionName = getCollectionName(entityClass);
        return processCosmosDBUpdate(query, update, entityClass);
    }

    @Override
    public UpdateResult updateMulti(Query query, UpdateDefinition update, String collectionName) {
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

    private UpdateResult processCosmosDBUpdate(Query query, UpdateDefinition update, Class<?> entityClass) {
        try {

            long total = count(query, entityClass);
            if (total == 0) {
                return UpdateResult.acknowledged(0, 0L, null);
            }

            if (total <= SMALL_BATCH_THRESHOLD) {
                try {
                    return super.updateMulti(query, update, entityClass);
                } catch (Exception e) {
                    log.debug("Failed to use native update for small batch, falling back to batch processing", e);
                }
            }

            return processBatchUpdate(query, update, entityClass, total);
        } catch (Exception e) {
            log.error("Error in CosmosDB update", e);
            throw e;
        }
    }

    private UpdateResult processBatchUpdate(Query query, UpdateDefinition update, Class<?> entityClass, long total) {
        long modifiedCount = 0;
        long matchedCount = 0;
        int batchSize = determineBatchSize(total);

        for (int skip = 0; skip < total; skip += batchSize) {
            Query batchQuery = Query.of(query).skip(skip).limit(batchSize);
            List<?> batch = find(batchQuery, entityClass);

            UpdateResult batchResult = processSingleBatch(batch, update, entityClass);
            matchedCount += batchResult.getMatchedCount();
            modifiedCount += batchResult.getModifiedCount();

            log.debug("Processed batch for {}: {}/{}", entityClass.getSimpleName(), skip + batch.size(), total);
        }

        return UpdateResult.acknowledged(matchedCount, modifiedCount, null);
    }

    private int determineBatchSize(long total) {
        if (total < 100) return 10;
        if (total < 1000) return 50;
        return DEFAULT_BATCH_SIZE;
    }

    private UpdateResult processSingleBatch(List<?> batch, UpdateDefinition update, Class<?> entityClass) {
        long modifiedCount = 0;
        long matchedCount = 0;

        for (Object doc : batch) {
            String id = getDocumentId(doc);
            if (id == null) continue;

            try {
                UpdateResult result =
                        super.updateFirst(Query.query(Criteria.where("_id").is(id)), update, entityClass);
                matchedCount += result.getMatchedCount();
                modifiedCount += result.getModifiedCount();
            } catch (Exception e) {
                log.error("Error updating document {}: {}", id, e.getMessage());
            }
        }

        return UpdateResult.acknowledged(matchedCount, modifiedCount, null);
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
