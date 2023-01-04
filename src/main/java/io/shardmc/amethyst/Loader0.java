package io.shardmc.amethyst;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Loader0 {

    private static final HashMap<String, String> REF = new HashMap<>();

    public static void prepare() throws Exception {
        for (Path file : Util.listFiles("/META-INF/meta/")) {
            Loader0.REF.put(file.getFileName().toString().replace(".meta", ""), Util.read(new FileInputStream(file.toFile())).toString());
        }
    }

    public static List<URL> extract(String subdir, String outputDir) throws IOException {
        Path dir = Paths.get(outputDir);
        if (!Files.exists(dir)) {
            Files.createDirectory(dir);
        }

        List<URL> urls = new ArrayList<>();
        for (Path file : Util.listFiles("/META-INF/" + subdir)) {
            String hash = Loader0.REF.get(file.getFileName().toString());
            Path output = dir.resolve(file.getFileName());

            System.out.println("Extracting: " + file.getFileName() + " to: " + output);

            if (Files.exists(output)) {
                try(InputStream stream = Files.newInputStream(file)) {
                    String actualHash = Util.getHash(stream);
                    if (actualHash.equalsIgnoreCase(hash)) {
                        stream.close();

                        urls.add(output.toUri().toURL());
                        continue;
                    }

                    System.out.printf("Expected file %s to have hash %s, but got %s%n", file, hash, actualHash);
                }
            }

            Files.copy(file, output, StandardCopyOption.REPLACE_EXISTING);
            urls.add(output.toUri().toURL());
        }

        return urls;
    }

    public static List<URL> readFrom(String subdir) throws IOException {
        Path subdirPath = Paths.get(subdir);
        List<URL> urls = new ArrayList<>();

        for(Path file : Util.listFiles(subdirPath)) {
            urls.add(file.toUri().toURL());
        }

        return urls;
    }

    public static URL[] extract() throws IOException {
        List<URL> urls = new ArrayList<>();
        urls.addAll(Loader0.extract("libs", "libraries"));
        urls.addAll(Loader0.extract("version", "version"));
        urls.addAll(Loader0.readFrom("modules"));

        return urls.toArray(new URL[0]);
    }

    public static void run(ClassLoader classLoader, String[] args) {
        Thread thread = new Thread(() -> {
            try {
                Util.getMain().invoke((Object) args);
                Class<?> mainClass = Class.forName("net.minecraft.server.Main", true, Loader.getClassLoader());
                MethodHandles.lookup().findStatic(
                        mainClass, "main", MethodType.methodType(void.class, String[].class)
                ).asFixedArity().invoke((Object) args);
            } catch (Throwable t) { Thrower.INSTANCE.sneakyThrow(t); }
        }, "ServerMain");
        thread.setContextClassLoader(Loader.getClassLoader());
        thread.start();
    }
}
