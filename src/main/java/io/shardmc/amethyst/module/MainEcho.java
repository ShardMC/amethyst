package io.shardmc.amethyst.module;

import io.shardmc.echo.annotations.Echo;
import io.shardmc.echo.annotations.Insert;

@Echo("net.minecraft.server.Main")
public class MainEcho {

    @Insert(method = "main", desc = "([Ljava/lang/String;)V")
    void main() {
        System.out.println("ECHOOO");
    }
}
