package io.shardmc.amethyst;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Loader {

    private static final List<URL> URLS = new ArrayList<>();
    private static final HashMap<String, String> REF = new HashMap<>();

    public static void prepare() throws IOException {
        for (Path file : Util.listFiles("/META-INF/meta/")) {
            Loader.REF.put(file.getFileName().toString().replace(".meta", ""), Files.readString(file));
        }
    }

    public static List<URL> extract(String subdir, String outputDir) throws IOException {
        Path dir = Paths.get(outputDir);
        if (!Files.exists(dir)) {
            Files.createDirectory(dir);
        }

        List<URL> urls = new ArrayList<>();
        for (Path file : Util.listFiles("/META-INF/" + subdir)) {
            String hash = Loader.REF.get(file.getFileName().toString());
            Path output = dir.resolve(file.getFileName());

            System.out.println("Extracting: " + file.getFileName() + " to: " + output);

            try(InputStream inputStream = Files.newInputStream(file)) {
                if (Files.exists(output)) {

                    String actualHash = Util.getHash(inputStream);
                    if (actualHash.equalsIgnoreCase(hash)) {
                        inputStream.close();

                        urls.add(output.toUri().toURL());
                        continue;
                    }

                    System.out.printf("Expected file %s to have hash %s, but got %s%n", file, hash, actualHash);
                }

                Files.copy(inputStream, output);
            }

            urls.add(output.toUri().toURL());
        }

        return urls;
    }

    public static List<URL> extract(String subdir) throws IOException {
        return Loader.extract(subdir, subdir);
    }

    public static List<URL> readFrom(String subdir) throws IOException {
        Path subdirPath = Paths.get(subdir);
        if (!Files.exists(subdirPath)) {
            Files.createDirectory(subdirPath);
        }

        List<URL> urls = new ArrayList<>();
        for(Path file : Util.listFiles(subdirPath)) {
            urls.add(file.toUri().toURL());
        }

        return urls;
    }

    public static void extract() throws IOException {
        List<URL> urls = new ArrayList<>();
        urls.addAll(Loader.extract("libraries"));
        urls.addAll(Loader.extract("versions"));
        urls.addAll(Loader.readFrom("modules"));

        Loader.URLS.addAll(urls);
    }

    public static void run(ClassLoader classLoader, String[] args) {
        Thread thread = new Thread(() -> {
            try {
                Class<?> mainClass = Class.forName("net.minecraft.server.Main", true, classLoader);
                MethodHandles.lookup().findStatic(
                        mainClass, "main", MethodType.methodType(void.class, String[].class)
                ).asFixedArity().invoke((Object) args);
            } catch (Throwable t) { Thrower.getInstance().sneakyThrow(t); }
        }, "ServerMain");
        thread.setContextClassLoader(classLoader);
        thread.start();
    }

    public static List<URL> getURLS() {
        return Loader.URLS;
    }
}
