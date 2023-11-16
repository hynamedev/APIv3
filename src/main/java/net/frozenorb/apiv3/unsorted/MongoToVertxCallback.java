package net.frozenorb.apiv3.unsorted;

import com.mongodb.async.SingleResultCallback;

import io.vertx.core.Future;

public final class MongoToVertxCallback<T> implements SingleResultCallback<T> {

	private final Future<T> future;

	public MongoToVertxCallback(Future<T> future) {
		this.future = future;
	}

	public void onResult(T val, Throwable error) {
		if (error != null) {
			future.fail(error);
		} else {
			future.complete(val);
		}
	}

}