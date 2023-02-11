package io.shardmc.amethyst;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class Util {

    public static String readResource(String resource) {
        try (InputStream input = Util.class.getResourceAsStream(resource)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static InputStream getResourceStream(String resource) {
        return Util.class.getResourceAsStream(resource);
    }

    public static File getResource(String resource) {
        try {
            return new File(Util.class.getResource(resource).toURI());
        } catch (URISyntaxException exception) {
            exception.printStackTrace();
            return null;
        }
    }
}