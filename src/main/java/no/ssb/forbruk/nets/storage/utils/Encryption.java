package no.ssb.forbruk.nets.storage.utils;

import no.ssb.forbruk.nets.storage.GoogleCloudStorage;
import no.ssb.rawdata.payload.encryption.EncryptionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Encryption {
    private static final Logger logger = LoggerFactory.getLogger(GoogleCloudStorage.class);

    static EncryptionClient encryptionClient;
    static byte[] secretKey;
    static boolean doEncrypt;

    String encryptionKey;
    String encryptionSalt;
    String encrypt;

    public Encryption(String encryptionKey, String encryptionSalt, String encrypt) {
        this.encryptionKey = encryptionKey;
        this.encryptionSalt = encryptionSalt;
        this.encrypt = encrypt;
        initialize();
    }

    private void initialize() {
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

    @Override
    public String toString() {
        return "Encryption{" +
                "encryptionKey=" + encryptionKey.length() +
                ", encryptionSalt=" + encryptionSalt.length() +
                ", encrypt=" + encrypt +
                '}';
    }
}
