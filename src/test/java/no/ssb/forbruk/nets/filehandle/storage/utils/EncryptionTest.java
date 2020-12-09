package no.ssb.forbruk.nets.filehandle.storage.utils;

import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EncryptionTest {
    private static final Logger logger = LoggerFactory.getLogger(EncryptionTest.class);


    @NonNull
    final Encryption encryption = new Encryption();

    final static String TEST_STRING = "This string will be encrypted.";
    final static String ENCRYPT_KEY = "IHfiuqsziUHTjgI0BnifhbTw1ZX6kCjntzzqKdUBExtNQWKC";
    final static String ENCRYPT_SALT = "iRlTQhtJDtt+cwQgh4NAz3dA8XEEvtOzXo/BfhU+Tno=";

    @Test
    void testEncrypt_doEncrypt_works() {
        initializeEncryption("true");

        byte[] encrypted = encryption.tryEncryptContent(TEST_STRING.getBytes());
        assertNotNull(encrypted, "Encrypted text schould not be null");
        assertNotEquals(new String(encrypted), TEST_STRING, "encrypted text should not be equal to test-string");
    }

    @Test
    void testEncrypt_notEncrypt_works() {
        initializeEncryption("false");

        byte[] encrypted = encryption.tryEncryptContent(TEST_STRING.getBytes());
        assertNotNull(encrypted, "Encrypted text schould not be null");
        assertEquals(new String(encrypted), TEST_STRING, "encrypted text should be equal to test-string");
    }

    @Test
    void testDecrypt_doEncrypt_works() {
        initializeEncryption("true");

        byte[] encrypted = encryption.tryEncryptContent(TEST_STRING.getBytes());
        byte[] decrypted = encryption.tryDecryptContent(encrypted);

        assertNotNull(decrypted, "Decrypted text schould not be null");
        assertNotEquals(new String(decrypted), new String(encrypted), "decrypted text should not be equal to encrypted text");
        assertEquals(new String(decrypted), TEST_STRING, "decrypted text should be equal to test-string");
    }

    @Test
    void testDecrypt_notEncrypt_works() {
        initializeEncryption("false");

        byte[] encrypted = encryption.tryEncryptContent(TEST_STRING.getBytes());
        byte[] decrypted = encryption.tryDecryptContent(encrypted);

        assertNotNull(decrypted, "Decrypted text schould not be null");
        assertEquals(new String(decrypted), new String(encrypted), "decrypted text should be equal to encrypted text");
        assertEquals(new String(decrypted), TEST_STRING, "decrypted text should be equal to test-string");
    }

    private void initializeEncryption(String encrypt) {
        ReflectionTestUtils.setField(encryption, "encryptionKey", ENCRYPT_KEY);
        ReflectionTestUtils.setField(encryption, "encryptionSalt", ENCRYPT_SALT);
        ReflectionTestUtils.setField(encryption, "encrypt", encrypt);
        encryption.setSecretKey();
        logger.info("encryption: {}", encryption.toString());
    }

}
