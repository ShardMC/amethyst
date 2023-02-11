package io.shardmc.amethyst;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

public class Hash {

    private static final MessageDigest digest = Hash.createDigest();

    private static MessageDigest createDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static String compute(InputStream stream) throws IOException {
        stream.transferTo(new DigestOutputStream(OutputStream.nullOutputStream(), digest));

        if (digest == null)
            return null;

        byte[] bytes = digest.digest();
        StringBuilder result = new StringBuilder(bytes.length * 2);

        for (byte b : bytes) {
            result.append(Character.forDigit(b >> 4 & 0xF, 16));
            result.append(Character.forDigit(b & 0xF, 16));
        }

        return result.toString();
    }
}
