package com.faforever.neroxis.exporter;

import com.faforever.neroxis.map.*;
import com.faforever.neroxis.util.Vector3f;

import java.io.*;
import java.nio.file.Path;
import java.util.List;

public strictfp class SaveExporter {

    public static File file;
    private static DataOutputStream out;

    public static void exportSave(Path folderPath, SCMap map) throws IOException {
        file = folderPath.resolve(map.getFilePrefix() + "_save.lua").toFile();
        boolean status = file.createNewFile();
        out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        out.writeBytes("Scenario = {\n");
        out.writeBytes("  next_area_id = '1',\n");
        out.writeBytes("  Props = {},\n");
        out.writeBytes("  Areas = {},\n");
        out.writeBytes("  MasterChain = {\n");
        out.writeBytes("    ['_MASTERCHAIN_'] = {\n");
        out.writeBytes("      Markers = {\n");
        for (Spawn spawn : map.getSpawns()) {
            out.writeBytes(String.format("        ['%s'] = {\n", spawn.getId()));
            out.writeBytes("          ['type'] = STRING( 'Blank Marker' ),\n");
            Vector3f v = spawn.getPosition();
            out.writeBytes(String.format("          ['position'] = VECTOR3( %s ),\n", v.toString()));
            out.writeBytes("          ['orientation'] = VECTOR3( 0.00, 0.00, 0.00 ),\n");
            out.writeBytes("          ['color'] = STRING( 'ff800080' ),\n");
            out.writeBytes("          ['prop'] = STRING( '/env/common/props/markers/M_Blank_prop.bp' ),\n");
            out.writeBytes("        },\n");
        }
        if (!map.isUnexplored()) {
            for (Marker mex : map.getMexes()) {
                out.writeBytes(String.format("        ['%s'] = {\n", mex.getId()));
                out.writeBytes("          ['size'] = FLOAT( 1.000000 ),\n");
                out.writeBytes("          ['resource'] = BOOLEAN( true ),\n");
                out.writeBytes("          ['amount'] = FLOAT( 100.000000 ),\n");
                out.writeBytes("          ['color'] = STRING( 'ff808080' ),\n");
                out.writeBytes("          ['type'] = STRING( 'Mass' ),\n");
                out.writeBytes("          ['prop'] = STRING( '/env/common/props/markers/M_Mass_prop.bp' ),\n");
                out.writeBytes("          ['orientation'] = VECTOR3( 0, 0, 0 ),\n");
                Vector3f v = mex.getPosition();
                out.writeBytes(String.format("          ['position'] = VECTOR3( %s ),\n", v.toString()));
                out.writeBytes("        },\n");
            }
            for (Marker hydro : map.getHydros()) {
                out.writeBytes(String.format("        ['%s'] = {\n", hydro.getId()));
                out.writeBytes("          ['size'] = FLOAT( 3.00 ),\n");
                out.writeBytes("          ['resource'] = BOOLEAN( true ),\n");
                out.writeBytes("          ['amount'] = FLOAT( 100.000000 ),\n");
                out.writeBytes("          ['color'] = STRING( 'ff808080' ),\n");
                out.writeBytes("          ['type'] = STRING( 'Hydrocarbon' ),\n");
                out.writeBytes("          ['prop'] = STRING( '/env/common/props/markers/M_Hydrocarbon_prop.bp' ),\n");
                out.writeBytes("          ['orientation'] = VECTOR3( 0, 0, 0 ),\n");
                Vector3f v = hydro.getPosition();
                out.writeBytes(String.format("          ['position'] = VECTOR3( %s ),\n", v.toString()));
                out.writeBytes("        },\n");
            }
        }
        for (Marker blankMarker : map.getBlankMarkers()) {
            out.writeBytes("        ['" + blankMarker.getId() + "'] = {\n");
            out.writeBytes("          ['type'] = STRING( 'Blank Marker' ),\n");
            Vector3f v = blankMarker.getPosition();
            out.writeBytes(String.format("          ['position'] = VECTOR3( %s ),\n", v.toString()));
            out.writeBytes("          ['orientation'] = VECTOR3( 0.00, 0.00, 0.00 ),\n");
            out.writeBytes("          ['color'] = STRING( 'ff800080' ),\n");
            out.writeBytes("          ['prop'] = STRING( '/env/common/props/markers/M_Blank_prop.bp' ),\n");
            out.writeBytes("        },\n");
        }
        savePathMarkers(map.getAirAIMarkers(), "Air Path Node", "ffffffff", "DefaultAir");
        savePathMarkers(map.getLandAIMarkers(), "Land Path Node", "ff00ff00", "DefaultLand");
        savePathMarkers(map.getAmphibiousAIMarkers(), "Amphibious Path Node", "ff00ffff", "DefaultAmphibious");
        savePathMarkers(map.getNavyAIMarkers(), "Water Path Node", "ff0000ff", "DefaultWater");
        saveAIMarkers(map.getLargeExpansionAIMarkers(), "Large Expansion Area", "ffff0080", "/env/common/props/markers/M_Expansion_prop.bp");
        saveAIMarkers(map.getLargeExpansionAIMarkers(), "Expansion Area", "ff008080", "/env/common/props/markers/M_Expansion_prop.bp");
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
        for (Army army : map.getArmies()) {
            saveArmy(army);
        }
        out.writeBytes("  },\n");
        out.writeBytes("}\n");

        out.flush();
        out.close();
    }

    private static void savePathMarkers(List<AIMarker> aiMarkers, String type, String color, String graph) throws IOException {
        for (AIMarker aiMarker : aiMarkers) {
            if (aiMarker.getNeighborCount() > 0) {
                out.writeBytes(String.format("        ['%s'] = {\n", aiMarker.getId()));
                out.writeBytes("          ['hint'] = BOOLEAN( true ),\n");
                out.writeBytes(String.format("          ['type'] = STRING( '%s' ),\n", type));
                out.writeBytes("          ['adjacentTo'] = STRING( '");
                aiMarker.getNeighbors().forEach(id -> {
                    try {
                        out.writeBytes(" " + id);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                out.writeBytes(" '),\n");
                out.writeBytes(String.format("          ['color'] = STRING( '%s' ),\n", color));
                out.writeBytes(String.format("          ['graph'] = STRING( '%s' ),\n", graph));
                out.writeBytes("          ['prop'] = STRING( '/env/common/props/markers/M_Path_prop.bp' ),\n");
                out.writeBytes("          ['orientation'] = VECTOR3( 0, 0, 0 ),\n");
                Vector3f v = aiMarker.getPosition();
                out.writeBytes(String.format("          ['position'] = VECTOR3( %s ),\n", v.toString()));
                out.writeBytes("        },\n");
            }
        }
    }

    private static void saveAIMarkers(List<AIMarker> aiMarkers, String type, String color, String prop) throws IOException {
        for (AIMarker aiMarker : aiMarkers) {
            out.writeBytes("        ['" + aiMarker.getId() + "'] = {\n");
            out.writeBytes("          ['hint'] = BOOLEAN( true ),\n");
            out.writeBytes("          ['color'] = STRING( '" + color + "' ),\n");
            out.writeBytes("          ['type'] = STRING( '" + type + "' ),\n");
            out.writeBytes("          ['prop'] = STRING( '" + prop + "' ),\n");
            out.writeBytes("          ['orientation'] = VECTOR3( 0, 0, 0 ),\n");
            Vector3f v = aiMarker.getPosition();
            out.writeBytes(String.format("          ['position'] = VECTOR3( %s),\n", v.toString()));
            out.writeBytes("        },\n");
        }
    }

    private static void saveArmy(Army army) throws IOException {
        out.writeBytes(String.format("    ['%s'] = {\n", army.getId()));
        out.writeBytes("      personality = '',\n");
        out.writeBytes("      plans = '',\n");
        out.writeBytes("      color = 0,\n");
        out.writeBytes("      faction = 0,\n");
        out.writeBytes("      Economy = {mass = 0, energy = 0},\n");
        out.writeBytes("      Alliances = {},\n");
        out.writeBytes("      ['Units'] = GROUP {\n");
        out.writeBytes("        orders = '',\n");
        out.writeBytes("        platoon = '',\n");
        out.writeBytes("        Units = {\n");
        for (Group group : army.getGroups()) {
            saveGroup(group);
        }
        out.writeBytes("        },\n");
        out.writeBytes("      },\n");
        out.writeBytes("      PlatoonBuilders = {\n");
        out.writeBytes("        next_platoon_builder_id = '0',\n");
        out.writeBytes("        Builders = {},\n");
        out.writeBytes("      },\n");
        out.writeBytes("    },\n");
    }

    private static void saveGroup(Group group) throws IOException {
        out.writeBytes(String.format("          ['%s'] = GROUP {\n", group.getId()));
        out.writeBytes("            orders = '',\n");
        out.writeBytes("            platoon = '',\n");
        out.writeBytes("            Units = {\n");
        for (Unit unit : group.getUnits()) {
            saveUnit(unit);
        }
        out.writeBytes("            },\n");
        out.writeBytes("          },\n");
    }

    private static void saveUnit(Unit unit) throws IOException {
        out.writeBytes(String.format("              ['%s'] = {\n", unit.getId()));
        out.writeBytes(String.format("	              type = '%s',\n", unit.getType()));
        out.writeBytes("			              orders = '',\n");
        out.writeBytes("			              platoon = '',\n");
        Vector3f v = unit.getPosition();
        out.writeBytes(String.format("			              Position = { %s },\n", v.toString()));
        float rot = unit.getRotation();
        out.writeBytes(String.format("			              Orientation = { 0, %f, 0 },\n", rot));
        out.writeBytes("              },\n");
    }
}
