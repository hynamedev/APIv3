package net.frozenorb.apiv3.service.geoip;

import com.mongodb.async.SingleResultCallback;

import org.springframework.stereotype.Service;

@Service
public interface GeoIpService {

    void lookupInfo(String ip, SingleResultCallback<GeoIpInfo> callback);

}