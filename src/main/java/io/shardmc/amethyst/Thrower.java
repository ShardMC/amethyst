package io.shardmc.amethyst;

@SuppressWarnings("unchecked")
public record Thrower<T extends Throwable>() {
    public static final Thrower<RuntimeException> INSTANCE = new Thrower<>();

    public void sneakyThrow(Throwable exception) throws T {
        throw (T) exception;
    }
}