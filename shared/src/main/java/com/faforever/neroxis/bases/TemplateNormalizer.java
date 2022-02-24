package com.faforever.neroxis.bases;

import com.faforever.neroxis.util.LuaLoader;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public strictfp class TemplateNormalizer {

    public static void main(String[] args) throws IOException {
        Path templatePath = Paths.get(args[0]);
        if (templatePath.getFileName().toString().contains(".lua")) {
            parseLua(templatePath);
        }
    }

    public static void parseLua(Path luaPath) throws IOException {
        LuaValue lua = LuaLoader.loadFile(luaPath);
        LuaTable units = lua.get("Units").checktable();
        float maxX = Float.MIN_VALUE;
        float minX = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE;
        LuaValue key = LuaValue.NIL;
        while (units.next(key) != LuaValue.NIL) {
            key = units.next(key).checkvalue(1);
            LuaValue unit = units.get(key);
            LuaTable posTable = unit.get("Position").checktable();
            maxX = StrictMath.max(maxX, posTable.get(1).tofloat());
            minX = StrictMath.min(minX, posTable.get(1).tofloat());
            maxY = StrictMath.max(maxY, posTable.get(3).tofloat());
            minY = StrictMath.min(minY, posTable.get(3).tofloat());
        }
        float centerX = (maxX + minX) / 2;
        float centerY = (maxY + minY) / 2;
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(luaPath.toFile())));
        out.writeBytes("Units = {\n");
        int count = 0;
        key = LuaValue.NIL;
        while (units.next(key) != LuaValue.NIL) {
            key = units.next(key).checkvalue(1);
            LuaValue unit = units.get(key);
            LuaTable posTable = unit.get("Position").checktable();
            out.writeBytes(String.format("\t['UNIT_%d'] = {\n", count));
            out.writeBytes(String.format("\t\ttype = '%s',\n", unit.get("type")));
            out.writeBytes("\t\torders = '',\n");
            out.writeBytes("\t\tplatoon = '',\n");
            out.writeBytes(String.format("\t\tPosition = { %f, 0, %f },\n", posTable.get(1).tofloat() - centerX, posTable.get(3).tofloat() - centerY));
            out.writeBytes("\t\tOrientation = { 0, 0, 0 },\n");
            out.writeBytes("\t},\n");
            count++;
        }
        out.writeBytes("}\n");
        out.flush();
        out.close();
    }
}
