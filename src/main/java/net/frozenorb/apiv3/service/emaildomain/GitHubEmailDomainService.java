package net.frozenorb.apiv3.service.emaildomain;

import com.google.common.collect.ImmutableSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

import io.vertx.core.http.HttpClient;

// no RestTemplate because we don't depend on spring-web yet
@Component
public final class GitHubEmailDomainService implements EmailDomainService {

    @Autowired private HttpClient httpsClient;
    private Set<String> bannedDomains = ImmutableSet.of();

    // 10 minutes, can't use TimeUnit expression in annotation
    @Scheduled(fixedRate = 10 * 60 * 1000)
    private void updateDomains() {
        httpsClient.get(443, "raw.githubusercontent.com", "/martenson/disposable-email-domains/master/disposable_email_blacklist.conf", (response) -> {
            response.bodyHandler(body -> bannedDomains = ImmutableSet.copyOf(body.toString().split("\n")));
            response.exceptionHandler(Throwable::printStackTrace);
        }).end();
    }

    @Override
    public boolean isBannedDomain(String email) {
        String[] split = email.split("@");
        String domain = split[1];

        return bannedDomains.contains(domain.toLowerCase());
    }

}