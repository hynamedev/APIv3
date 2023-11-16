package net.frozenorb.apiv3.serialization.gson;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

public final class FollowAnnotationExclusionStrategy implements ExclusionStrategy {

	@Override
	public boolean shouldSkipField(FieldAttributes fieldAttributes) {
		return fieldAttributes.getAnnotation(ExcludeFromReplies.class) != null;
	}

	@Override
	public boolean shouldSkipClass(Class<?> aClass) {
		return false;
	}

}