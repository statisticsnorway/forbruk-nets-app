package no.ssb.forbruk.nets.filehandle.storage.utils;

import no.ssb.forbruk.nets.filehandle.storage.GoogleCloudStorage;
import no.ssb.rawdata.payload.encryption.EncryptionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Encryption {
    private static final Logger logger = LoggerFactory.getLogger(GoogleCloudStorage.class);

    static EncryptionClient encryptionClient;
    static byte[] secretKey;
    static boolean doEncrypt;

    String encrypt;

    public Encryption(String encryptionKey, String encryptionSalt, String encrypt) {
        logger.info("key: {}", encryptionKey);
        logger.info("salt: {}", encryptionSalt);
        logger.info("encrypt: {}", encrypt);
        this.encrypt = encrypt;
        initialize(encryptionKey, encryptionSalt);
    }

    private void initialize(String encryptionKey, String encryptionSalt) {
        encryptionClient = new EncryptionClient();
        doEncrypt = encrypt != null && Boolean.parseBoolean(encrypt);
        secretKey = doEncrypt ? encryptionClient.generateSecretKey(
                encryptionKey.toCharArray(),
                encryptionSalt.getBytes()).getEncoded() : null;
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
                "encrypt=" + encrypt +
                '}';
    }
}
