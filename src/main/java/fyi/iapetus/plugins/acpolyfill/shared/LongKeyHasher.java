package fyi.iapetus.plugins.acpolyfill.shared;

import org.apache.commons.codec.digest.DigestUtils;

public class LongKeyHasher {
    private static final Integer MAX_KEY_LENGTH = Integer.valueOf(100);

    public static String hashKeyIfTooLong(String key) {
        if (key.length() > MAX_KEY_LENGTH.intValue()) {
            String keyHash = DigestUtils.md5Hex(key);
            String keptOriginalKey = key.substring(0, MAX_KEY_LENGTH.intValue() - keyHash.length());
            String hashedKey = keptOriginalKey + keyHash;
            return hashedKey;
        }
        return key;
    }
}
