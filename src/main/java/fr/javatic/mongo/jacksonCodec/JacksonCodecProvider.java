package fr.javatic.mongo.jacksonCodec;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

public class JacksonCodecProvider implements CodecProvider {

    private final ObjectMapper bsonObjectMapper;

    public JacksonCodecProvider(final ObjectMapper bsonObjectMapper) {
        this.bsonObjectMapper = bsonObjectMapper;
    }

    @Override
    public <T> Codec<T> get(final Class<T> type, final CodecRegistry registry) {
        if (type.getAnnotationsByType(Entity.class).length > 0) {
            return new JacksonCodec<>(bsonObjectMapper, registry, type);
        } else {
            return null;
        }
    }

}