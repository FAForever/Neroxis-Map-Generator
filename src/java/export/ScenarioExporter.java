package export;

import map.SCMap;

import java.io.*;
import java.nio.file.Path;

public strictfp class ScenarioExporter {

    public static void exportScenario(Path folderPath, String mapname, SCMap map) throws IOException {
        File file = folderPath.resolve(mapname).resolve(mapname + "_scenario.lua").toFile();
        boolean status = file.createNewFile();
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        out.writeBytes("version = 3\n");
        out.writeBytes("ScenarioInfo = {\n");
        out.writeBytes("  name = '" + mapname + "',\n");
        out.writeBytes("  description = '<LOC Empty_Description>',\n");
        out.writeBytes("  type = 'skirmish',\n");
        out.writeBytes("  starts = true,\n");
        out.writeBytes("  preview = '',\n");
        out.writeBytes("  size = {" + map.getSize() + ", " + map.getSize() + "},\n");
        out.writeBytes("  map = '/maps/" + mapname + "/" + mapname + ".scmap',\n");
        out.writeBytes("  map_version = 1,\n");
        out.writeBytes("  save = '/maps/" + mapname + "/" + mapname + "_save.lua',\n");
        out.writeBytes("  script = '/maps/" + mapname + "/" + mapname + "_script.lua',\n");
        out.writeBytes("  norushradius = 50,\n");
        out.writeBytes("  Configurations = {\n");
        out.writeBytes("    ['standard'] = {\n");
        out.writeBytes("      teams = {\n");
        out.writeBytes("        {\n");
        out.writeBytes("          name = 'FFA',\n");
        out.writeBytes("          armies = {");
        for (int i = 0; i < map.getSpawns().length; i++) {
            out.writeBytes("'ARMY_" + (i + 1) + "'");
            if (i < map.getSpawns().length - 1)
                out.writeBytes(",");
        }
        out.writeBytes("},\n");
        out.writeBytes("        },\n");
        out.writeBytes("      },\n");
        out.writeBytes("      customprops = {\n");
        out.writeBytes("        ['ExtraArmies'] = STRING( 'ARMY_17 NEUTRAL_CIVILIAN' ),\n");
        out.writeBytes("      },\n");
        out.writeBytes("    },\n");
        out.writeBytes("  },\n");
        for (int i = 0; i < map.getSpawns().length; i++) {
            out.writeBytes("  norushoffsetX_ARMY_" + (i + 1) + " = 0,\n");
            out.writeBytes("  norushoffsetY_ARMY_" + (i + 1) + " = 0,\n");
        }
        out.writeBytes("}\n");

        out.flush();
        out.close();
    }
}
