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
    static boolean doEncrypt;

    @Value("#{environment.forbruk_nets_encryption_key}")
    private String encryptionKey;
    @Value("#{environment.forbruk_nets_encryption_salt}")
    private String encryptionSalt;
    @Value("${google.storage.encryption}")
    private String encrypt;

    public Encryption() {
    }

    public Encryption(String encryptionKey, String encryptionSalt, String encrypt) {
        this.encryptionKey = encryptionKey;
        this.encryptionSalt = encryptionSalt;
        this.encrypt = encrypt;
    }

    public void initialize() {
        encryptionClient = new EncryptionClient();
        secretKey = encryptionClient.generateSecretKey(
                encryptionKey.toCharArray(),
                encryptionSalt.getBytes()).getEncoded();
        doEncrypt = encrypt != null && Boolean.parseBoolean(encrypt);
        logger.info("encrypt: {}, doEncrypt: {}", encrypt, doEncrypt);
    }

    public byte[] tryEncryptContent(byte[] content) {
        if (doEncrypt && secretKey != null) {
            byte[] iv = encryptionClient.generateIV();
            return encryptionClient.encrypt(secretKey, iv, content);
        }
        return content;
    }

    public byte[] tryDecryptContent(byte[] content) {
        if (doEncrypt && secretKey != null && content != null) {
            return encryptionClient.decrypt(secretKey, content);
        }
        return content;
    }
}
