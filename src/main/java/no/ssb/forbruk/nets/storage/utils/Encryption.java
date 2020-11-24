package no.ssb.forbruk.nets.storage.utils;

import no.ssb.forbruk.nets.storage.GoogleCloudStorage;
import no.ssb.rawdata.payload.encryption.EncryptionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class Encryption {
    private static final Logger logger = LoggerFactory.getLogger(GoogleCloudStorage.class);

    static EncryptionClient encryptionClient;
    static byte[] secretKey;

    @Value("#{environment.forbruk_nets_encryption_key}")
    private String encryptionKey;
    @Value("#{environment.forbruk_nets_encryption_salt}")
    private String encryptionSalt;

    public void initialize() {
//        logger.info("encryptionKey: {}", encryptionKey);
//        logger.info("encryptionSalt: {}", encryptionSalt);
        encryptionClient = new EncryptionClient();
        secretKey = encryptionClient.generateSecretKey(
                encryptionKey.toCharArray(),
                encryptionSalt.getBytes()).getEncoded();
//        logger.info("secretKey: {}", secretKey);
    }

    public byte[] tryEncryptContent(byte[] content) {
        if (secretKey != null) {
            byte[] iv = encryptionClient.generateIV();
            return encryptionClient.encrypt(secretKey, iv, content);
        }
        return content;
    }

    public byte[] tryDecryptContent(byte[] content) {
        if (secretKey != null && content != null) {
            return encryptionClient.decrypt(secretKey, content);
        }
        return content;
    }
}
