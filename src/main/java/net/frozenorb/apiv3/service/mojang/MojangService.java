package net.frozenorb.apiv3.service.mojang;

import com.mongodb.async.SingleResultCallback;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface MojangService {

    void getName(UUID id, SingleResultCallback<String> callback);

}