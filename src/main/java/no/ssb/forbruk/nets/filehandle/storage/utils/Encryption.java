package no.ssb.forbruk.nets.filehandle.storage.utils;

import no.ssb.forbruk.nets.filehandle.storage.GoogleCloudStorage;
import no.ssb.rawdata.payload.encryption.EncryptionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

public class Encryption {
    private static final Logger logger = LoggerFactory.getLogger(GoogleCloudStorage.class);

    private final static EncryptionClient encryptionClient = new EncryptionClient();

    static byte[] secretKey;

//    String encryptionKey;
//    String encryptionSalt;
//    String encrypt;

    @Value("#{environment.forbruk_nets_encryption_key}")
    private String encryptionKey;
    @Value("#{environment.forbruk_nets_encryption_salt}")
    private String encryptionSalt;
    @Value("${google.storage.encryption}")
    private String encrypt;

//    public Encryption(String encryptionKey, String encryptionSalt, String encrypt) {
//        this.encryptionKey = encryptionKey;
//        this.encryptionSalt = encryptionSalt;
//        this.encrypt = encrypt;
//        initialize();
//    }

    public void setSecretKey() {
        secretKey = generateSecretKey(
                encryptionKey,
                encryptionSalt);
    }

    public byte[] generateSecretKey(String key, String  salt) {
        return (key != null && salt != null) ?
                encryptionClient.generateSecretKey(key.toCharArray(), salt.getBytes()).getEncoded() :
                null;

    }

    public byte[] tryEncryptContent(byte[] content) {
        if (doEncrypt() && secretKey != null) {
            byte[] iv = encryptionClient.generateIV();
            return encryptionClient.encrypt(secretKey, iv, content);
        }
        return content;
    }

    public byte[] tryDecryptContent(byte[] content) {
        if (doEncrypt() && secretKey != null && content != null) {
            return encryptionClient.decrypt(secretKey, content);
        }
        return content;
    }

    private boolean doEncrypt() {
        return encrypt != null && Boolean.parseBoolean(encrypt);
    }

    @Override
    public String toString() {
        return "Encryption{" +
                "encryptionKey=" + (encryptionKey != null ? encryptionKey.substring(2,3) : " null") +
                ", encryptionSalt=" + (encryptionSalt  != null ? encryptionSalt.substring(1,3) : " null") +
                ", encrypt=" + encrypt +
                ", secretKey=" + new String(secretKey).substring(3,5) +
                '}';
    }
}
