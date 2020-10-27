package no.ssb.forbruk.nets.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import no.ssb.forbruk.nets.service.AppSecretsService;

@Service
public class ReadinessService {

    private AppSecretsService appSecretsService;

    @Autowired
    public ReadinessService(AppSecretsService appSecretsService){this.appSecretsService = appSecretsService;}

    private static final Logger logger = LoggerFactory.getLogger(ReadinessService.class);

    public boolean isReady(){
        // This might not be best practice, since no traffic gets routed to the service, but it depends on
        // your service. This might lead to cascading errors in other services, and one should consider
        // queueing requests.
        return isSecretAvailable();
    }

    private boolean isSecretAvailable() {
        String secret = appSecretsService.getNetsSecret();
        return secret != null && secret.length() != 0;
    }
}
