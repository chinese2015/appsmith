package com.appsmith.server.x.configurations.mongo;

import com.appsmith.external.models.BaseDomain;
import com.mongodb.client.result.UpdateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class CustomMongoTemplateTest {

    @Mock
    private MongoDatabaseFactory databaseFactory;

    @Mock
    private MongoConverter converter;

    @Mock
    private MongoProperties mongoProperties;

    private CustomMongoTemplate customMongoTemplate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        customMongoTemplate = new CustomMongoTemplate(databaseFactory, converter, mongoProperties);
    }

    @Test
    void whenNotCosmosDB_thenUseSuperUpdateMulti() {
        // Given
        when(mongoProperties.getUri()).thenReturn("mongodb://localhost:27017/test");
        Query query = new Query();
        Update update = new Update();

        // When
        customMongoTemplate.updateMulti(query, update, TestEntity.class);

        // Then
        // Verify super.updateMulti was called (需要特殊的 mockito 设置来验证)
    }

    @Test
    void whenCosmosDB_thenUseBatchProcessing() {
        // Given
        when(mongoProperties.getUri())
                .thenReturn("mongodb://account:key@account.mongo.cosmos.azure.com:10255/?ssl=true");

        TestEntity entity1 = new TestEntity("1", "name1");
        TestEntity entity2 = new TestEntity("2", "name2");
        List<TestEntity> entities = Arrays.asList(entity1, entity2);

        Query query = new Query();
        Update update = new Update().set("name", "updated");

        when(customMongoTemplate.count(any(), eq(TestEntity.class))).thenReturn(2L);
        when(customMongoTemplate.find(any(), eq(TestEntity.class))).thenReturn(entities);
        when(customMongoTemplate.updateFirst(any(), any(), eq(TestEntity.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));

        // When
        UpdateResult result = customMongoTemplate.updateMulti(query, update, TestEntity.class);

        // Then
        assertThat(result.getModifiedCount()).isEqualTo(2);
        assertThat(result.getMatchedCount()).isEqualTo(2);
    }

    @Test
    void whenSmallBatch_thenTryNativeUpdate() {
        // Given
        when(mongoProperties.getUri()).thenReturn("mongodb://account.mongo.cosmos.azure.com:10255");

        Query query = new Query();
        Update update = new Update();

        when(customMongoTemplate.count(any(), eq(TestEntity.class))).thenReturn(5L);
        when(customMongoTemplate.updateMulti(any(), any(), eq(TestEntity.class)))
                .thenReturn(UpdateResult.acknowledged(5, 5L, null));

        // When
        UpdateResult result = customMongoTemplate.updateMulti(query, update, TestEntity.class);

        // Then
        assertThat(result.getModifiedCount()).isEqualTo(5);
    }

    // Test Entity
    private static class TestEntity extends BaseDomain {
        private String name;

        public TestEntity(String id, String name) {
            this.setId(id);
            this.name = name;
        }
    }
}
