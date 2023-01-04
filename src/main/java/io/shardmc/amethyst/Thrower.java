package io.shardmc.amethyst;

@SuppressWarnings("unchecked")
public record Thrower<T extends Throwable>() {
    private static final Thrower<RuntimeException> INSTANCE = new Thrower<>();

    public void sneakyThrow(Throwable exception) throws T {
        throw (T) exception;
    }

    public static Thrower<RuntimeException> getInstance() {
        return INSTANCE;
    }
}