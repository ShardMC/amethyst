package io.shardmc.amethyst;

import java.util.ArrayList;
import java.util.List;

public class Amethyst {

    public static void main(String[] args) {
        Amethyst.run(new ArrayList<>(List.of(args)));
    }

    private static void run(List<String> argv) {
        /*try {
            Loader.run(() -> {
                try {
                    Loader.getClassLoader();
                    //Echo.prepare();
                    Util.getMain().invoke((Object) Util.toArray(argv));
                } catch (Throwable t) { Thrower.INSTANCE.sneakyThrow(t); }
            });
        } catch (Exception exception) {
            exception.printStackTrace();
        }*/
    }
}
