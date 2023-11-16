package net.frozenorb.apiv3.serialization.mongodb;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.UUID;

public final class UuidCodecProvider implements CodecProvider {

	public <T> Codec<T> get(Class<T> clazz, CodecRegistry codecRegistry) {
		if (clazz == UUID.class) {
			return (Codec<T>) new UuidCodec();
		} else {
			return null;
		}
	}

}