package no.ssb.forbruk.nets.filehandle.storage.utils;

import lombok.NonNull;
import lombok.Setter;
import no.ssb.forbruk.nets.filehandle.storage.GoogleCloudStorage;
import no.ssb.rawdata.payload.encryption.EncryptionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Encryption {
    private static final Logger logger = LoggerFactory.getLogger(GoogleCloudStorage.class);

    private final static EncryptionClient encryptionClient = new EncryptionClient();


    @Value("#{environment.forbruk_nets_encryption_key}")
    private String encryptionKey;
    @Value("#{environment.forbruk_nets_encryption_salt}")
    private String encryptionSalt;
    @Value("${google.storage.encryption}")
    private String encrypt;

    @Setter
    static byte[] secretKey;
    @Setter
    boolean doEncrypt;


    public void setEncryptionValues() {
        secretKey = generateSecretKey(
                encryptionKey,
                encryptionSalt);
        doEncrypt = doEncrypt(encrypt);
    }

    private byte[] generateSecretKey(String key, String  salt) {
        return (key != null && salt != null) ?
                encryptionClient.generateSecretKey(key.toCharArray(), salt.getBytes()).getEncoded() :
                null;
    }

    private boolean doEncrypt(String encrypt) {
        return encrypt != null && Boolean.parseBoolean(encrypt);
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
                "encryptionKey=" + (encryptionKey != null ? encryptionKey.substring(2,3) : " null") +
                ", encryptionSalt=" + (encryptionSalt  != null ? encryptionSalt.substring(1,3) : " null") +
                ", encrypt=" + encrypt +
                ", secretKey=" + new String(secretKey).substring(3,5) +
                '}';
    }
}
