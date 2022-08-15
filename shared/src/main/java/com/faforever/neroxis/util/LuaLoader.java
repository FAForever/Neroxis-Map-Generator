package com.faforever.neroxis.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

public final class LuaLoader {
    private LuaLoader() {
        throw new AssertionError("Not instantiatable");
    }

    public static LuaValue loadFile(Path file) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file)) {
            return load(inputStream);
        }
    }

    public static LuaValue load(InputStream inputStream) throws IOException {
        Globals globals = buildEnvironment();
        globals.load(inputStream, "@" + inputStream.hashCode(), "bt", globals).invoke();
        return globals;
    }

    private static Globals buildEnvironment() throws IOException {
        Globals globals = JsePlatform.standardGlobals();
        try (InputStream inputStream = LuaLoader.class.getResourceAsStream("/lua/faf.lua")) {
            globals.baselib.load(globals.load(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)));
            return globals;
        }
    }
}
