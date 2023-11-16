package net.frozenorb.apiv3.unsorted;

import com.mongodb.async.SingleResultCallback;

public final class MongoToVoidMongoCallback<T> implements SingleResultCallback<T> {

	private final SingleResultCallback<Void> wrapped;

	public MongoToVoidMongoCallback(SingleResultCallback<Void> wrapped) {
		this.wrapped = wrapped;
	}

	public void onResult(T ignored, Throwable error) {
		wrapped.onResult(null, error);
	}

}