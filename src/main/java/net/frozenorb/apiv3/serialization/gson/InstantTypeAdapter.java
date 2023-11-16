package net.frozenorb.apiv3.serialization.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;

public final class InstantTypeAdapter extends TypeAdapter<Instant> {

	public void write(JsonWriter writer, Instant write) throws IOException {
		if (write == null) {
			writer.nullValue();
		} else {
			//writer.value(write.toEpochMilli() - System.currentTimeMillis());
			writer.value(write.toEpochMilli());
		}
	}

	// This is used with Gson, which is only used
	// to serialize outgoing responses, thus we
	// don't need to have a read method.
	public Instant read(JsonReader reader) {
		throw new IllegalArgumentException();
	}

}