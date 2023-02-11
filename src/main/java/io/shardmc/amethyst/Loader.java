package io.shardmc.amethyst;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Loader {

    private static final List<URL> URLS = new ArrayList<>();
    private static final HashMap<String, String> REF = new HashMap<>();


    public static void run(ClassLoader loader, String[] args) {
        Thread thread = new Thread(() -> {
            try {
                Class<?> mainClass = Class.forName("net.minecraft.server.Main", true, loader);
                MethodHandles.lookup().findStatic(
                        mainClass, "main", MethodType.methodType(void.class, String[].class)
                ).asFixedArity().invoke((Object) args);
            } catch (Throwable t) { Thrower.getInstance().sneakyThrow(t); }
        }, "ServerMain");
        thread.setContextClassLoader(loader);
        thread.start();
    }

    public static void discover() {
        System.out.println("[Amethyst] Discovering...");

        String meta = Util.readResource("/META-INF/.meta");

        if (meta == null) {
            System.out.println("Failed to read metadata!");
            return;
        }

        meta.lines().forEach(s -> {
            String[] data = s.split(" ");
            String hash = data[0];
            String path = data[1];

            Loader.getRef().put(path, hash);
        });
    }

    public static void extract() throws IOException {
        List<URL> urls = new ArrayList<>();
        urls.addAll(Loader.extract("libraries"));
        urls.addAll(Loader.extract("versions"));
        urls.addAll(Loader.readFrom("modules"));

        Loader.URLS.addAll(urls);
    }

    public static List<URL> readFrom(String subdir) throws IOException {
        System.out.println("[Amethyst] Reading files from: " + subdir);

        Path subdirPath = Paths.get(subdir);
        if (!Files.exists(subdirPath)) {
            Files.createDirectory(subdirPath);
        }

        List<URL> urls = new ArrayList<>();
        try (Stream<Path> stream = Files.list(subdirPath)) {
            stream.forEach(path -> {
                try {
                    urls.add(path.toUri().toURL());
                } catch (MalformedURLException e) {
                    System.out.println("Failed to read files from directory: " + subdirPath);
                }
            });
        }

        return urls;
    }

    public static List<URL> extract(String subdir) throws IOException {
        return Loader.extract(subdir, subdir);
    }

    public static List<URL> extract(String subdir, String outputDir) throws IOException {
        System.out.println("[Amethyst] Extracting files... " + subdir + "-" + outputDir);

        Path dir = Paths.get(outputDir);
        if (!Files.exists(dir)) {
            Files.createDirectory(dir);
        }

        List<URL> urls = new ArrayList<>();
        for (Map.Entry<String, String> entry : Loader.getRef().entrySet()) {
            String file = entry.getKey();
            String hash = entry.getValue();

            String[] fileStructure = file.split("/");
            Path output = dir.resolve(fileStructure[fileStructure.length - 1]);

            System.out.println("[Amethyst] Extracting: " + file + " to: " + output);

            try(InputStream stream = Util.getResourceStream(file)) {
                if (Files.exists(output)) {
                    String actualHash = Hash.compute(stream);
                    if (actualHash == null) {
                        System.out.println("Failed to compute hash! (Note: this error doesn't mean that the server files are not trustworthy)");
                        continue;
                    }

                    if (actualHash.equalsIgnoreCase(hash)) {
                        urls.add(output.toUri().toURL());
                        stream.close();

                        continue;
                    }

                    System.out.printf("Expected file %s to have hash %s, but got %s%n", file, hash, actualHash);
                }

                Files.copy(stream, output);
            }

            urls.add(output.toUri().toURL());
        }

        return urls;
    }

    public static List<URL> getURLS() {
        return Loader.URLS;
    }

    public static Map<String, String> getRef() {
        return Loader.REF;
    }
}
