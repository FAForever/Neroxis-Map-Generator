package export;

import java.io.*;

import map.*;

public strictfp class ScriptExporter {

    public static void exportScript(String folderPath, String mapname, SCMap map) throws IOException {
		File file = new File(folderPath + mapname + File.separator + mapname + "_script.lua");
		file.createNewFile();
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
