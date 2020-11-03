package no.ssb.forbruk.nets.service;
import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Based on https://cloud.google.com/secret-manager/docs/creating-and-accessing-secrets#secretmanager-create-secret-java
 */
@Service
public class AppSecretsService {
    private static final Logger logger = LoggerFactory.getLogger(AppSecretsService.class);

    @Value("${projectname:forbruk}")
    private String projectName;
    @Value("${bip.gcp.secrets.projectname:forbruk}")
    private String projectId;
    @Getter
    @Value("${bip.gcp.secrets.nets.ftp.privkey.name:NETS_SECRET}")
    private String netsSecretName;
    @Getter
    @Value("${bip.gcp.secrets.nets.ftp.privkey.version:latest}")
    private String netsSecretVersion;
    @Getter
    @Value("${bip.gcp.secrets.nets.ftp.passphrase.name:NETS_PASSPHRASE}")
    private String netsPassphraseName;
    @Getter
    @Value("${bip.gcp.secrets.nets.ftp.passphrase.version:latest}")
    private String netsPassphraseVersion;


    private String netsSecret = "";
    private String netsPassphrase = "";

    // Define other secrets here

    @Autowired
    public AppSecretsService() {

    }

    /**
     * Demosecret
     * <p>
     * The secret is "static" defined this way, which means that this application must
     * be restarted if a secret is rotated.
     * To get the secret every time just call the "accessSecretVersion"-method each time.
     * This might, on the other hand, cause overhead if the secret is fetched every time its needed.
     * One must consider how often the secret is needed vs how often the secret is rotated and a restart
     * must be performed.
     *
     * @return Returns the secret data for secret netsSecret
     */
    public String getNetsSecret() {
        if (isEmptySecret(netsSecret))
            netsSecret = getSecretVersionData(netsSecretName, netsSecretVersion);
        return netsSecret;
    }

    public String getNetsPassphrase() {
        if (isEmptySecret(netsPassphrase))
            netsPassphrase = getSecretVersionData(netsPassphraseName, netsPassphraseVersion);
        return netsPassphrase;
    }

    /**
     * Access the payload for the given secret version if one exists. The version
     * can be a version number as a string (e.g. "5") or an alias (e.g. "latest").
     *
     * @param secretName    Name of the secret
     * @param secretVersion Version of the secret
     * @return The secret data as a String
     */
    private String getSecretVersionData(String secretName, String secretVersion) {
        //Edge-logging since we are invoking an external API
//        logger.info("projectId: {}, netsSecretName: {}, netsSecretVersion: {}", projectId, netsSecretName, netsSecretVersion);

        MDC.put("nets-appname", projectId);
        MDC.put("nets-secret-name", secretName);
        MDC.put("nets-secret-version", secretVersion);

        logger.info("Trying to access secret");

        AccessSecretVersionResponse response;
        String data = "";
        try {
            SecretManagerServiceClient client = SecretManagerServiceClient.create();

            SecretVersionName secretVersionName = SecretVersionName.of(projectId, secretName, secretVersion);
            // Access the secret version.
            response = client.accessSecretVersion(secretVersionName);
            data = response.getPayload().getData().toStringUtf8();
            if (isEmptySecret(data))
                logger.error("Error accessing " + netsSecretName + ", version " + secretVersion);

            client.close();
        } catch (IOException e) {
            logger.error("Error creating SecretManagerServiceClient", e);
        }
        logger.info("Secret accessed OK");
        MDC.clear();
        return data;
    }

    /**
     * Checks if the secret contains any data
     *
     * @param secretData The data to check
     * @return true if secret is empty or null, false otherwise
     */
    private boolean isEmptySecret(String secretData) {
        return secretData == null || secretData.equalsIgnoreCase("");
    }
}
