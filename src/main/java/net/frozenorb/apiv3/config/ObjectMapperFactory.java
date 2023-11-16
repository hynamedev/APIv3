package net.frozenorb.apiv3.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.undercouch.bson4jackson.BsonFactory;
import de.undercouch.bson4jackson.BsonParser;

public class ObjectMapperFactory {
    private ObjectMapperFactory() {
    }

    public static ObjectMapper createObjectMapper() {
        BsonFactory bsonFactory = new BsonFactory();
        bsonFactory.enable(BsonParser.Feature.HONOR_DOCUMENT_LENGTH);

        return new ObjectMapper(bsonFactory);
    }
}
