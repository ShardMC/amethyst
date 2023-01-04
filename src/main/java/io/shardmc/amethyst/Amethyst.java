package io.shardmc.amethyst;

import io.shardmc.echo.core.Echo;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

public class Amethyst {

    public static void main(String[] args) throws IOException {
        Loader.prepare();
        Loader.extract();

        ClassLoader classLoader = new URLClassLoader(
                Loader.getURLS().toArray(new URL[0]), Echo.getClassLoader()
        );

        Thread.currentThread().setContextClassLoader(classLoader);

        Echo.load("shard");
        Echo.finish();

        Loader.run(classLoader, args);
    }
}
