package net.frozenorb.apiv3.util;

import com.google.common.util.concurrent.SettableFuture;

import com.mongodb.async.SingleResultCallback;

import java.util.concurrent.ExecutionException;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SyncUtils {

	public static <T> SingleResultCallback<T> vertxWrap(SingleResultCallback<T> callback) {
		Context context = SpringUtils.getBean(Vertx.class).getOrCreateContext();

		return (result, error) -> {
			context.runOnContext((ignored) -> {
				callback.onResult(result, error);
			});
		};
	}

	public static <T> T runBlocking(BlockingWrapper<T> wrapper) {
		SettableFuture<T> future = SettableFuture.create();

		wrapper.run((result, error) -> {
			if (error != null) {
				future.setException(error);
			} else {
				future.set(result);
			}
		});

		try {
			return future.get();
		} catch (InterruptedException | ExecutionException ex) {
			// No matter what we get we'll just rethrow.
			throw new RuntimeException(ex);
		}
	}

	public interface BlockingWrapper<T> {

		void run(SingleResultCallback<T> v);

	}

}