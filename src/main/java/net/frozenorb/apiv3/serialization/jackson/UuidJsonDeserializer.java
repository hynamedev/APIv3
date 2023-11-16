package net.frozenorb.apiv3.serialization.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.UUID;

public final class UuidJsonDeserializer extends JsonDeserializer<UUID> {

	@Override
	public UUID deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
		return UUID.fromString(jsonParser.getValueAsString());
	}

}