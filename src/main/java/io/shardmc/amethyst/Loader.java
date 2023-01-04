package io.shardmc.amethyst;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class Loader {

    private static URLClassLoader classLoader;

    public static void initClassLoader(ClassLoader classLoader) {
        Loader.classLoader = new URLClassLoader(Loader.extractFiles(), classLoader);
    }

    public static URLClassLoader getClassLoader() {
        return Loader.classLoader;
    }

    public static void run(ClassLoader classLoader, String[] args) {
        Loader.initClassLoader(classLoader);

        Thread thread = new Thread(() -> {
            try {
                Util.getMain().invoke((Object) args);
            } catch (Throwable t) { Thrower.INSTANCE.sneakyThrow(t); }
        }, "ServerMain");

        thread.setContextClassLoader(Loader.getClassLoader());
        thread.start();
    }

    public static URL[] extractFiles() {
        try {
            String repoDir = System.getProperty("bundlerRepoDir", "");
            Path outputDir = Paths.get(repoDir);
            Files.createDirectories(outputDir);

            List<URL> extractedUrls = new ArrayList<>();
            Loader.readAndExtractDir("versions", outputDir, extractedUrls);
            Loader.readAndExtractDir("libraries", outputDir, extractedUrls);
            Loader.readFrom("modules", outputDir, extractedUrls);

            return extractedUrls.toArray(new URL[0]);
        } catch (Exception exception) {
            exception.printStackTrace(System.out);
            System.out.println("Failed to extract server libraries, exiting");
            System.exit(-1);
        }

        return null;
    }

    private static void readFrom(String subdir, Path outputDir, List<URL> extractedUrls) {
        Path subdirPath = outputDir.resolve(subdir);
        try {
            for(File file : subdirPath.toFile().listFiles()) {
                extractedUrls.add(file.toURI().toURL());
            }
        } catch (Exception ignored) { }
    }

    private static <T> T readResource(String resource, ResourceParser<T> parser) throws Exception {
        String fullPath = "/META-INF/" + resource;
        InputStream is = Loader.class.getResourceAsStream(fullPath);
        try {
            if (is == null)
                throw new IllegalStateException("Resource " + fullPath + " not found");
            T t = parser.parse(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)));
            is.close();
            return t;
        } catch (Exception throwable) {
            if(is != null) {
                try {
                    is.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
            }
            throw throwable;
        }
    }

    private static void readAndExtractDir(String subdir, Path outputDir, List<URL> extractedUrls) throws Exception {
        List<FileEntry> entries = Loader.readResource(subdir + ".list", reader -> reader.lines().map(FileEntry::parseLine).toList());
        Path subdirPath = outputDir.resolve(subdir);
        for (FileEntry entry : entries) {
            Path outputFile = subdirPath.resolve(entry.path);
            Loader.checkAndExtractJar(subdir, entry, outputFile);
            extractedUrls.add(outputFile.toUri().toURL());
        }
    }

    private static void checkAndExtractJar(String subdir, FileEntry entry, Path outputFile) throws Exception {
        if (!Files.exists(outputFile) || !Loader.checkIntegrity(outputFile, entry.hash())) {
            Loader.extractJar(subdir, entry.path, outputFile);
        }
    }

    private static void extractJar(String subdir, String jarPath, Path outputFile) throws IOException {
        Files.createDirectories(outputFile.getParent());
        InputStream input = Loader.class.getResourceAsStream("/META-INF/" + subdir + "/" + jarPath);
        try {
            if (input == null)
                throw new IllegalStateException("Declared library " + jarPath + " not found");
            Files.copy(input, outputFile, StandardCopyOption.REPLACE_EXISTING);
            input.close();
        } catch (Throwable throwable) {
            if (input != null)
                try {
                    input.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
            throw throwable;
        }
    }

    private static boolean checkIntegrity(Path file, String expectedHash) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        InputStream output = Files.newInputStream(file);
        try {
            output.transferTo(new DigestOutputStream(OutputStream.nullOutputStream(), digest));
            String actualHash = Util.byteToHex(digest.digest());
            if (actualHash.equalsIgnoreCase(expectedHash)) {
                output.close();
                return true;
            }

            System.out.printf("Expected file %s to have hash %s, but got %s%n", file, expectedHash, actualHash);
            output.close();
        } catch (Throwable throwable) {
            try {
                output.close();
            } catch (Throwable throwable1) {
                throwable.addSuppressed(throwable1);
            }
            throw throwable;
        }
        return false;
    }

    @FunctionalInterface
    private interface ResourceParser<T> {
        T parse(BufferedReader param1BufferedReader) throws Exception;
    }

    private record FileEntry(String hash, String id, String path) {
        public static FileEntry parseLine(String line) {
            String[] fields = line.split("\t");
            if (fields.length != 3)
                throw new IllegalStateException("Malformed library entry: " + line);
            return new FileEntry(fields[0], fields[1], fields[2]);
        }
    }
}