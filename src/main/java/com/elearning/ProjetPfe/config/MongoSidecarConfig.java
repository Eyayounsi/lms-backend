package com.elearning.ProjetPfe.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@ConditionalOnProperty(prefix = "app.mongo", name = "enabled", havingValue = "true")
@EnableMongoRepositories(basePackages = "com.elearning.ProjetPfe.mongo.repository")
public class MongoSidecarConfig {
}
