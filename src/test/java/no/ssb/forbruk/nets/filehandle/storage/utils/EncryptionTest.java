package no.ssb.forbruk.nets.filehandle.storage.utils;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EncryptionTest {

    Encryption encryption;

    final static String TEST_STRING = "This string will be encrypted.";
    final static String ENCRYPT_KEY = "IHfiuqsziUHTjgI0BnifhbTw1ZX6kCjntzzqKdUBExtNQWKC";
    final static String ENCRYPT_SALT = "iRlTQhtJDtt+cwQgh4NAz3dA8XEEvtOzXo/BfhU+Tno=";

    @Test
    void testEncrypt_doEncrypt_works() {
        encryption = new Encryption(ENCRYPT_KEY, ENCRYPT_SALT,"true");

        byte[] encrypted = encryption.tryEncryptContent(TEST_STRING.getBytes());
        assertNotNull(encrypted, "Encrypted text schould not be null");
        assertNotEquals(new String(encrypted), TEST_STRING, "encrypted text should not be equal to test-string");
    }

    @Test
    void testEncrypt_notEncrypt_works() {
        encryption = new Encryption(ENCRYPT_KEY, ENCRYPT_SALT,"false");

        byte[] encrypted = encryption.tryEncryptContent(TEST_STRING.getBytes());
        assertNotNull(encrypted, "Encrypted text schould not be null");
        assertEquals(new String(encrypted), TEST_STRING, "encrypted text should be equal to test-string");
    }

    @Test
    void testDecrypt_doEncrypt_works() {
        encryption = new Encryption(ENCRYPT_KEY, ENCRYPT_SALT,"true");

        byte[] encrypted = encryption.tryEncryptContent(TEST_STRING.getBytes());
        byte[] decrypted = encryption.tryDecryptContent(encrypted);

        assertNotNull(decrypted, "Decrypted text schould not be null");
        assertNotEquals(new String(decrypted), new String(encrypted), "decrypted text should not be equal to encrypted text");
        assertEquals(new String(decrypted), TEST_STRING, "decrypted text should be equal to test-string");
    }

    @Test
    void testDecrypt_notEncrypt_works() {
        encryption = new Encryption(ENCRYPT_KEY, ENCRYPT_SALT,"false");

        byte[] encrypted = encryption.tryEncryptContent(TEST_STRING.getBytes());
        byte[] decrypted = encryption.tryDecryptContent(encrypted);

        assertNotNull(decrypted, "Decrypted text schould not be null");
        assertEquals(new String(decrypted), new String(encrypted), "decrypted text should be equal to encrypted text");
        assertEquals(new String(decrypted), TEST_STRING, "decrypted text should be equal to test-string");
    }

}
