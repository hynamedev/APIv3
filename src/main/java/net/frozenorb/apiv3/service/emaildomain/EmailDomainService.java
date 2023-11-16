package net.frozenorb.apiv3.service.emaildomain;

import org.springframework.stereotype.Service;

@Service
public interface EmailDomainService {

    boolean isBannedDomain(String email);

}