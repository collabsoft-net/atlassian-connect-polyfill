package fyi.iapetus.plugins.acpolyfill.shared;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LongKeyHasher {
    private static final Integer MAX_KEY_LENGTH = Integer.valueOf(100);

    public static String hashKeyIfTooLong(String key) throws NoSuchAlgorithmException {
        if (key.length() > MAX_KEY_LENGTH.intValue()) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(key.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            String keyHash = DatatypeConverter.printHexBinary(digest).toUpperCase();

            String keptOriginalKey = key.substring(0, MAX_KEY_LENGTH.intValue() - keyHash.length());
            String hashedKey = keptOriginalKey + keyHash;
            return hashedKey;
        }
        return key;
    }
}
