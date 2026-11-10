package com.faforever.neroxis.exporter;

import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Spawn;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

public class ScenarioExporter {
    public static void exportScenario(Path folderPath, SCMap map) throws IOException {
        Optional<SCMap.Reclaim> reclaim = map.getReclaim();
        if (reclaim.isEmpty()) {
            System.err.println("Warning: Reclaim totals omitted because prop or wreckage reclaim metadata is missing.");
        }
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
        if (reclaim.isPresent()) {
            out.writeBytes(String.format(Locale.ROOT, "  reclaim = {%.2f, %.0f},\n",
                                        reclaim.get().mass(), reclaim.get().energy()));
        }
        out.writeBytes("  map = '/maps/" + mapFolder + "/" + mapPrefix + ".scmap',\n");
        out.writeBytes("  map_version = 1,\n");
        out.writeBytes("  save = '/maps/" + mapFolder + "/" + mapPrefix + "_save.lua',\n");
        out.writeBytes("  script = '/maps/" + mapFolder + "/" + mapPrefix + "_script.lua',\n");
        if (!map.isGeneratePreview()) {
            out.writeBytes("  hidePreviewMarkers = " + true + ",\n");
        }
        out.writeBytes("  norushradius = " + map.getNoRushRadius() + ",\n");
        for (Spawn spawn : map.getSpawns()) {
            out.writeBytes("  norushoffsetX_" + spawn.getId() + " = " + spawn.getNoRushOffset().x() + ",\n");
            out.writeBytes("  norushoffsetY_" + spawn.getId() + " = " + spawn.getNoRushOffset().y() + ",\n");
        }
        out.writeBytes("  Configurations = {\n");
        out.writeBytes("    ['standard'] = {\n");
        out.writeBytes("      teams = {\n");
        out.writeBytes("        {\n");
        out.writeBytes("          name = 'FFA',\n");
        out.writeBytes("          armies = {");
        for (Spawn spawn : map.getSpawns()) {
            out.writeBytes("'" + spawn.getId() + "',");
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
