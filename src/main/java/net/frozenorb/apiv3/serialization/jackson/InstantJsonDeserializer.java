package net.frozenorb.apiv3.serialization.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;

public final class InstantJsonDeserializer extends JsonDeserializer<Instant> {

	@Override
	public Instant deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
		return Instant.ofEpochMilli(jsonParser.getValueAsLong());
	}

}