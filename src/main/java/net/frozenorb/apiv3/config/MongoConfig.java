package net.frozenorb.apiv3.config;

import com.google.common.collect.ImmutableList;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mongodb.ConnectionString;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClientSettings;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoDatabase;
import com.mongodb.client.model.IndexModel;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.connection.ServerSettings;
import com.mongodb.connection.SocketSettings;
import com.mongodb.connection.SslSettings;

import net.frozenorb.apiv3.serialization.jackson.InstantJsonDeserializer;
import net.frozenorb.apiv3.serialization.jackson.InstantJsonSerializer;
import net.frozenorb.apiv3.serialization.jackson.UuidJsonDeserializer;
import net.frozenorb.apiv3.serialization.jackson.UuidJsonSerializer;
import net.frozenorb.apiv3.serialization.mongodb.UuidCodecProvider;

import org.bson.Document;
import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.DocumentCodecProvider;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.UUID;

import fr.javatic.mongo.jacksonCodec.JacksonCodecProvider;
import fr.javatic.mongo.jacksonCodec.ObjectMapperFactory;

@Configuration
public class MongoConfig {

    @Bean
    public MongoDatabase mongoDatabase(@Value("${mongoUri}") String mongoUri) {
        ConnectionString connStr = new ConnectionString(mongoUri);

        // all of these lines except for .codecRegistry are copied from MongoClients#create(ConnectionString)
        MongoClient mongoClient = MongoClients.create(MongoClientSettings.builder()
                .clusterSettings(ClusterSettings.builder().applyConnectionString(connStr).build())
                .connectionPoolSettings(ConnectionPoolSettings.builder().applyConnectionString(connStr).build())
                .serverSettings(ServerSettings.builder().build()).credentialList(connStr.getCredentialList())
                .sslSettings(SslSettings.builder().applyConnectionString(connStr).build())
                .socketSettings(SocketSettings.builder().applyConnectionString(connStr).build())
                .codecRegistry(CodecRegistries.fromProviders(ImmutableList.of(
                        new UuidCodecProvider(), // MHQ, fixes uuid serialization
                        new ValueCodecProvider(),
                        new DocumentCodecProvider(),
                        new BsonValueCodecProvider(),
                        new JacksonCodecProvider(createMongoJacksonMapper()) // Jackson codec, provides serialization/deserialization
                )))
                .build()
        );

        MongoDatabase database = mongoClient.getDatabase(connStr.getDatabase());

        database.getCollection("auditLog").createIndexes(ImmutableList.of(
                new IndexModel(new Document("user", 1)),
                new IndexModel(new Document("performedAt", 1)),
                new IndexModel(new Document("type", 1))
        ), (a, b) -> {});
        database.getCollection("grants").createIndexes(ImmutableList.of(
                new IndexModel(new Document("user", 1)),
                new IndexModel(new Document("rank", 1)),
                new IndexModel(new Document("addedAt", 1))
        ), (a, b) -> {});
        database.getCollection("ipLog").createIndexes(ImmutableList.of(
                new IndexModel(new Document("user", 1)),
                new IndexModel(new Document("user", 1).append("userIp", 1)),
                new IndexModel(new Document("hashedUserIp", 1))
        ), (a, b) -> {});
        database.getCollection("ipBans").createIndexes(ImmutableList.of(
                new IndexModel(new Document("userIp", 1))
        ), (a, b) -> {});
        database.getCollection("ipIntel").createIndexes(ImmutableList.of(
                new IndexModel(new Document("hashedIp", 1)),
                new IndexModel(new Document("location", "2dsphere"))
        ), (a, b) -> {});
        database.getCollection("punishments").createIndexes(ImmutableList.of(
                new IndexModel(new Document("user", 1)),
                new IndexModel(new Document("type", 1)),
                new IndexModel(new Document("addedAt", 1)),
                new IndexModel(new Document("addedBy", 1)),
                new IndexModel(new Document("linkedIpBanId", 1))
        ), (a, b) -> {});
        database.getCollection("users").createIndexes(ImmutableList.of(
                new IndexModel(new Document("lastUsername", 1)),
                new IndexModel(new Document("lastUsernameLower", 1)),
                new IndexModel(new Document("emailToken", 1)),
                new IndexModel(new Document("totpSecret", 1))
        ), (a, b) -> {});
        database.getCollection("userMeta").createIndexes(ImmutableList.of(
                new IndexModel(new Document("user", 1).append("serverGroup", 1))
        ), (a, b) -> {});

        return database;
    }

    private ObjectMapper createMongoJacksonMapper() {
        ObjectMapper mongoJacksonMapper = ObjectMapperFactory.createObjectMapper();
        SimpleModule module = new SimpleModule();

        module.addSerializer(Instant.class, new InstantJsonSerializer());
        module.addDeserializer(Instant.class, new InstantJsonDeserializer());
        module.addSerializer(UUID.class, new UuidJsonSerializer());
        module.addDeserializer(UUID.class, new UuidJsonDeserializer());

        mongoJacksonMapper.registerModule(module);
        mongoJacksonMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mongoJacksonMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mongoJacksonMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        return mongoJacksonMapper;
    }

}