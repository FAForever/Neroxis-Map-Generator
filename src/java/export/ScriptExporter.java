package export;

import map.SCMap;

import java.io.*;
import java.nio.file.Path;

public strictfp class ScriptExporter {

    public static void exportScript(Path folderPath, String mapname, SCMap map) throws IOException {
        File file = folderPath.resolve(mapname + "_script.lua").toFile();
        boolean status = file.createNewFile();
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        out.writeBytes("local ScenarioUtils = import('/lua/sim/ScenarioUtilities.lua')\n");
        out.writeBytes("function OnPopulate()\n");
        out.writeBytes("ScenarioUtils.InitializeArmies()\n");
        out.writeBytes("end\n");
        out.writeBytes("function OnStart(self)\n");
        out.writeBytes("end");

        out.flush();
        out.close();
    }
}
