package com.faforever.neroxis.exporter;

import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Spawn;

import java.io.*;
import java.nio.file.Path;

public class ScenarioExporter {
    public static void exportScenario(Path folderPath, SCMap map) throws IOException {
        String mapPrefix = map.getFilePrefix();
        File file = folderPath.resolve(mapPrefix + "_scenario.lua").toFile();
        String mapFolder = folderPath.getFileName().toString();
        boolean status = file.createNewFile();
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        out.writeBytes("version = 3\n");
        out.writeBytes("ScenarioInfo = {\n");
        out.writeBytes("  name = \"" + map.getName() + "\",\n");
        out.writeBytes("  description = \"" + map.getDescription() + "\",\n");
        out.writeBytes("  type = 'skirmish',\n");
        out.writeBytes("  starts = true,\n");
        out.writeBytes("  preview = '',\n");
        out.writeBytes("  size = {" + map.getSize() + ", " + map.getSize() + "},\n");
        out.writeBytes("  map = '/maps/" + mapFolder + "/" + mapPrefix + ".scmap',\n");
        out.writeBytes("  map_version = 1,\n");
        out.writeBytes("  save = '/maps/" + mapFolder + "/" + mapPrefix + "_save.lua',\n");
        out.writeBytes("  script = '/maps/" + mapFolder + "/" + mapPrefix + "_script.lua',\n");
        if (!map.isGeneratePreview()) {
            out.writeBytes("  hidePreviewMarkers = " + !map.isGeneratePreview() + ",\n");
        }
        out.writeBytes("  norushradius = " + map.getNoRushRadius() + ",\n");
        for (Spawn spawn : map.getSpawns()) {
            out.writeBytes("  norushoffsetX_" + spawn.getId() + " = " + spawn.getNoRushOffset().getX() + ",\n");
            out.writeBytes("  norushoffsetY_" + spawn.getId() + " = " + spawn.getNoRushOffset().getY() + ",\n");
        }
        out.writeBytes("  Configurations = {\n");
        out.writeBytes("    ['standard'] = {\n");
        out.writeBytes("      teams = {\n");
        out.writeBytes("        {\n");
        out.writeBytes("          name = 'FFA',\n");
        out.writeBytes("          armies = {");
        for (Spawn spawn : map.getSpawns()) {
            out.writeBytes("'" + spawn.getId() + "'");
            if (map.getSpawns().indexOf(spawn) < map.getSpawns().size() - 1) {
                out.writeBytes(",");
            }
        }
        out.writeBytes("},\n");
        out.writeBytes("        },\n");
        out.writeBytes("      },\n");
        out.writeBytes("      customprops = {\n");
        out.writeBytes("        ['ExtraArmies'] = STRING( 'ARMY_17 NEUTRAL_CIVILIAN' ),\n");
        out.writeBytes("      },\n");
        out.writeBytes("    },\n");
        out.writeBytes("  },\n");
        out.writeBytes("}\n");

        out.flush();
        out.close();
    }
}
