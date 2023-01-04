package io.shardmc.amethyst;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.stream.Stream;

public class Util {

    private static final MessageDigest digest = Util.createDigest();

    public static MessageDigest createDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("all")
    public static String getHash(InputStream stream) throws IOException {
        stream.transferTo(new DigestOutputStream(OutputStream.nullOutputStream(), digest));
        return Util.byteToHex(digest.digest());
    }

    public static String byteToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            result.append(Character.forDigit(b >> 4 & 0xF, 16));
            result.append(Character.forDigit(b & 0xF, 16));
        }

        return result.toString();
    }

    public static Path[] listFiles(Path resource) throws IOException {
        try (Stream<Path> list = Files.list(resource)) {
            return list.toArray(Path[]::new);
        }
    }

    public static Path[] listFiles(String resource) throws IOException {
        return Util.listFiles(Paths.get(Util.getResource(resource)));
    }

    @SuppressWarnings("all")
    public static URI getResource(String resource) {
        return Util.safeToUri(Util.class.getResource(resource));
    }

    public static URI safeToUri(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException exception) {
            exception.printStackTrace(); // Well, fuck
            return null;
        }
    }
}