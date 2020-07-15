package export;

import map.SCMap;
import util.Vector3f;

import java.io.*;
import java.nio.file.Path;

public strictfp class SaveExporter {

    public static File file;
    private static DataOutputStream out;

    public static void exportSave(Path folderPath, String mapname, SCMap map) throws IOException {
        file = folderPath.resolve(mapname).resolve(mapname + "_save.lua").toFile();
        boolean status = file.createNewFile();
        out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        out.writeBytes("Scenario = {\n");
        out.writeBytes("  next_area_id = '1',\n");
        out.writeBytes("  Props = {},\n");
        out.writeBytes("  Areas = {},\n");
        out.writeBytes("  MasterChain = {\n");
        out.writeBytes("    ['_MASTERCHAIN_'] = {\n");
        out.writeBytes("      Markers = {\n");
        for (int i = 0; i < map.getSpawns().length; i++) {
            out.writeBytes("        ['ARMY_" + (i + 1) + "'] = {\n");
            out.writeBytes("          ['type'] = STRING( 'Blank Marker' ),\n");
            out.writeBytes("          ['position'] = VECTOR3( " + (map.getSpawns()[i].x + 0.5f) + ", " + map.getSpawns()[i].y + ", " + (map.getSpawns()[i].z + 0.5f) + " ),\n");
            out.writeBytes("          ['orientation'] = VECTOR3( 0.00, 0.00, 0.00 ),\n");
            out.writeBytes("          ['color'] = STRING( 'ff800080' ),\n");
            out.writeBytes("          ['prop'] = STRING( '/env/common/props/markers/M_Blank_prop.bp' ),\n");
            out.writeBytes("        },\n");
        }
        for (int i = 0; i < map.getMexes().length; i++) {
            if (map.getMexes()[i] != null) {
                out.writeBytes("        ['MASS_" + (i + 1) + "'] = {\n");
                out.writeBytes("          ['size'] = FLOAT( 1.000000 ),\n");
                out.writeBytes("          ['resource'] = BOOLEAN( true ),\n");
                out.writeBytes("          ['amount'] = FLOAT( 100.000000 ),\n");
                out.writeBytes("          ['color'] = STRING( 'ff808080' ),\n");
                out.writeBytes("          ['type'] = STRING( 'Mass' ),\n");
                out.writeBytes("          ['prop'] = STRING( '/env/common/props/markers/M_Mass_prop.bp' ),\n");
                out.writeBytes("          ['orientation'] = VECTOR3( 0, 0, 0 ),\n");
                out.writeBytes("          ['position'] = VECTOR3( " + (map.getMexes()[i].x + 0.5f) + ", " + map.getMexes()[i].y + ", " + (map.getMexes()[i].z + 0.5f) + " ),\n");
                out.writeBytes("        },\n");
            }
        }
        for (int i = 0; i < map.getHydros().length; i++) {
            if (map.getHydros()[i] != null) {
                out.writeBytes("        ['Hydrocarbon_" + (i + 1) + "'] = {\n");
                out.writeBytes("          ['size'] = FLOAT( 3.00 ),\n");
                out.writeBytes("          ['resource'] = BOOLEAN( true ),\n");
                out.writeBytes("          ['amount'] = FLOAT( 100.000000 ),\n");
                out.writeBytes("          ['color'] = STRING( 'ff808080' ),\n");
                out.writeBytes("          ['type'] = STRING( 'Hydrocarbon' ),\n");
                out.writeBytes("          ['prop'] = STRING( '/env/common/props/markers/M_Hydrocarbon_prop.bp' ),\n");
                out.writeBytes("          ['orientation'] = VECTOR3( 0, 0, 0 ),\n");
                out.writeBytes("          ['position'] = VECTOR3( " + (map.getHydros()[i].x + 0.5f) + ", " + map.getHydros()[i].y + ", " + (map.getHydros()[i].z + 0.5f) + " ),\n");
                out.writeBytes("        },\n");
            }
        }
        out.writeBytes("      },\n");
        out.writeBytes("    },\n");
        out.writeBytes("  },\n");
        out.writeBytes("  Chains = {},\n");
        out.writeBytes("  next_queue_id = '1',\n");
        out.writeBytes("  Orders = {},\n");
        out.writeBytes("  next_platoon_id = '1',\n");
        out.writeBytes("  Platoons = {},\n");
        out.writeBytes("  next_army_id = '1',\n");
        out.writeBytes("  next_group_id = '1',\n");
        out.writeBytes("  next_unit_id = '1',\n");
        out.writeBytes("  Armies = {\n");
        for (int i = 0; i < map.getSpawns().length; i++) {
            saveArmy("ARMY_" + (i + 1), map);
        }
        saveArmy("ARMY_17", map);
        saveArmy("NEUTRAL_CIVILIAN", map);
        out.writeBytes("  },\n");
        out.writeBytes("}\n");

        out.flush();
        out.close();
    }

    private static void saveArmy(String name, SCMap map) throws IOException {
        out.writeBytes("    ['" + name + "'] = {\n");
        out.writeBytes("      personality = '',\n");
        out.writeBytes("      plans = '',\n");
        out.writeBytes("      color = 0,\n");
        out.writeBytes("      faction = 0,\n");
        out.writeBytes("      Economy = {mass = 0, energy = 0},\n");
        out.writeBytes("      Alliances = {},\n");
        out.writeBytes("      ['Units'] = GROUP {\n");
        out.writeBytes("        orders = '',\n");
        out.writeBytes("        platoon = '',\n");
        if (name.equals("ARMY_17")) {
            out.writeBytes("        Units = {\n");
            out.writeBytes("          ['INITIAL'] = GROUP {\n");
            out.writeBytes("            orders = '',\n");
            out.writeBytes("            platoon = '',\n");
            out.writeBytes("            Units = {\n");
            for (int i = 0; i < map.getUnitCount(); i++) {
                saveUnit(map.getUnit(i), i);
            }
            out.writeBytes("            },\n");
            out.writeBytes("          },\n");
            out.writeBytes("          ['WRECKAGE'] = GROUP {\n");
            out.writeBytes("            orders = '',\n");
            out.writeBytes("            platoon = '',\n");
            out.writeBytes("            Units = {\n");
            for (int i = 0; i < map.getWreckCount(); i++) {
                saveUnit(map.getWreck(i), i);
            }
            out.writeBytes("            },\n");
            out.writeBytes("          },\n");
        } else {
            out.writeBytes("        Units = {\n");
            out.writeBytes("          ['INITIAL'] = GROUP {\n");
            out.writeBytes("            orders = '',\n");
            out.writeBytes("            platoon = '',\n");
            out.writeBytes("            Units = {},\n");
            out.writeBytes("          },\n");
        }
        out.writeBytes("        },\n");
        out.writeBytes("      },\n");
        out.writeBytes("      PlatoonBuilders = {\n");
        out.writeBytes("        next_platoon_builder_id = '0',\n");
        out.writeBytes("        Builders = {},\n");
        out.writeBytes("      },\n");
        out.writeBytes("    },\n");
    }

    private static void saveUnit(map.Unit unit, int i) throws IOException {
        out.writeBytes(String.format("['UNIT_%d'] = {\n", i));
        out.writeBytes(String.format("	type = '%s',\n", unit.getType()));
        out.writeBytes("			orders = '',\n");
        out.writeBytes("			platoon = '',\n");
        Vector3f v = unit.getPosition();
        out.writeBytes(String.format("			Position = { %f, %f, %f },\n", v.x, v.y, v.z));
        float rot = unit.getRotation();
        out.writeBytes(String.format("			Orientation = { 0, %f, 0 },\n", rot));
        out.writeBytes("},\n");
    }
}
