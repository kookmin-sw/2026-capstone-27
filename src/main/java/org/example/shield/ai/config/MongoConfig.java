package org.example.shield.ai.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB 설정.
 * rag.retrieval.stub=false 일 때만 활성화되어 MongoDB 연결 및 Repository 스캔 수행.
 * 기본값(RAG_STUB=true)에서는 이 설정이 로드되지 않으므로 MongoDB 연결을 시도하지 않음.
 *
 * MongoAutoConfiguration은 ShieldApplication에서 전역 exclude 되었으므로,
 * 여기서 수동으로 MongoClient, MongoDatabaseFactory, MongoTemplate 빈을 생성.
 */
@Configuration
@ConditionalOnProperty(name = "rag.retrieval.stub", havingValue = "false")
@EnableMongoRepositories(basePackages = "org.example.shield.ai.infrastructure")
public class MongoConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Bean
    public MongoClient mongoClient() {
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongoUri))
                .build();
        return MongoClients.create(settings);
    }

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
        return new SimpleMongoClientDatabaseFactory(mongoClient, databaseName);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTemplate(mongoDatabaseFactory);
    }
}
