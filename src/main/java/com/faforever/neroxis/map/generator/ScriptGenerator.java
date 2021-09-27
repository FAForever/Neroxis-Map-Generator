package com.faforever.neroxis.map.generator;

import com.faforever.neroxis.map.Marker;
import com.faforever.neroxis.map.Prop;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.util.Vector3;

public strictfp class ScriptGenerator {

    public static void generateScript(SCMap map) {
        if (map.isUnexplored()) {
            map.setScript(generateUnexploredScript(map));
        } else {
            map.setScript(generateDefaultScript());
        }
    }

    private static String generateDefaultScript() {
        return "local ScenarioUtils = import('/lua/sim/ScenarioUtilities.lua')\n" +
                "local ScenarioFramework = import('/lua/ScenarioFramework.lua')\n" +
                "function OnPopulate()\n" +
                "ScenarioUtils.InitializeArmies()\n" +
                "ScenarioFramework.SetPlayableArea('AREA_1' , false)\n" +
                "end\n" +
                "function OnStart(self)\n" +
                "end";
    }

    private static String generateUnexploredScript(SCMap map) {
        int mapSize = map.getSize();
        int decalSize = mapSize * mapSize / 8192;
        int checkDecalRange = decalSize / 2 + 24;
        int checkResourceRange = decalSize / 2 + 32;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("local mexLocations = {");
        for (Marker mex : map.getMexes()) {
            Vector3 v = mex.getPosition();
            stringBuilder.append(String.format("{position = VECTOR3( %s ), marked = false},", v.toString()));
        }
        stringBuilder.append("}\n");
        stringBuilder.append("local hydroLocations = {");
        for (Marker hydro : map.getHydros()) {
            Vector3 v = hydro.getPosition();
            stringBuilder.append(String.format("{position = VECTOR3( %s ), marked = false},", v.toString()));
        }
        stringBuilder.append("}\n");
        stringBuilder.append("local propLocations = {");
        for (Prop prop : map.getProps()) {
            Vector3 v = prop.getPosition();
            stringBuilder.append(String.format("{position = VECTOR3( %s ), bp = '%s', heading = %f, marked = false},", v.toString(), prop.getPath(), prop.getRotation()));
        }
        stringBuilder.append("}\n");
        stringBuilder.append("local ScenarioFramework = import('/lua/ScenarioFramework.lua')\n" +
                "local ScenarioUtils = import('/lua/sim/ScenarioUtilities.lua')\n" +
                "local myBrain = nil\n" +
                "local decals = {}\n");
        stringBuilder.append(String.format("local decalSpacing = %d;\n", decalSize));
        stringBuilder.append(String.format("local decalSize = %d;\n", decalSize));
        stringBuilder.append(String.format("local checkDecalRange = %d;\n", checkDecalRange));
        stringBuilder.append(String.format("local checkRange = %d;\n", checkResourceRange));
        stringBuilder.append("\n" +
                "function OnPopulate()\n" +
                "\tScenarioUtils.InitializeArmies()\n" +
                "end\n" +
                "\n" +
                "function OnStart(self)\n");
        stringBuilder.append(String.format("\tScenarioFramework.SetPlayableArea(Rect( %1$d - 4, %1$d - 4, %1$d + 4, %1$d + 4 ) , false)\n", mapSize / 2));
        stringBuilder.append(String.format("\tScenarioInfo.MapData.PlayableRect = {0, 0, %1$d, %1$d}\n", mapSize));
        stringBuilder.append("\tAddFogDecals()\n" +
                "\tScenarioFramework.CreateTimerTrigger(MapExpand, 1, true)\n" +
                "\tlocal player = GetFocusArmy()\n" +
                "\tmyBrain = nil\n" +
                "\tfor _, v in ArmyBrains do\n" +
                "\t\tif v:GetArmyIndex() == player then\n" +
                "\t\t\tmyBrain = v\n" +
                "\t\t\tbreak\n" +
                "\t\tend\n" +
                "\tend\n" +
                "end\n" +
                "\n" +
                "function MapExpand()\n");
        stringBuilder.append(String.format("\tScenarioFramework.SetPlayableArea(Rect( 0, 0, %1$d, %1$d) , false)\n", mapSize));
        stringBuilder.append("\tCheckDecals()\n" +
                "\tCheckMexes()\n" +
                "\tCheckHydros()\n" +
                "\tCheckProps()\n" +
                "end\n" +
                "\n" +
                "function AddFogDecals()\n" +
                "\tlocal count = 0\n" +
                "\tlocal decal = CreateDecal({0, 0, 0}, 0, \"/env/Common/decals/NoSpec_spec.dds\", \"\", \"Water Albedo\", 1, 1, 10000, 0, -1, 0)\n" +
                "\tlocal position = {0,0,0}\n" +
                "\tlocal decalInfo = {decal = decal, position = position}\n" +
                "\tcount = count + 1\n" +
                "\ttable.insert(decals, decalInfo)\n" +
                "\tfor i=decalSize / 2,ScenarioInfo.size[1],decalSpacing do\n" +
                "\t\tfor j=decalSize / 2,ScenarioInfo.size[2],decalSpacing do\n" +
                "\t\t\tlocal decal = CreateDecal({i, 0, j}, 0, \"/env/Common/decals/NoSpec_spec.dds\", \"\", \"Water Albedo\", decalSize * 1.125, decalSize * 1.125, 10000, 0, -1, -1)\n" +
                "\t\t\tlocal position = {i,0,j}\n" +
                "\t\t\tlocal decalInfo = {decal = decal, position = position}\n" +
                "\t\t\tcount = count + 1\n" +
                "\t\t\ttable.insert(decals, decalInfo)\n" +
                "\t\tend\n" +
                "\tend\n" +
                "\ttable.setn(decals, count + 1)\n" +
                "end\n" +
                "\n" +
                "function CheckDecals()\n" +
                "\tForkThread(function()\n" +
                "\t\twhile(table.getn(decals) > 1) do\n" +
                "\t\t\tfor i,decal in decals do\n" +
                "\t\t\t\tif decal ~= nil then\n" +
                "\t\t\t\t\tlocal upos = decal.position\n" +
                "\t\t\t\t\tif upos ~= nil then\n" +
                "\t\t\t\t\t\tlocal units = nil\n" +
                "\t\t\t\t\t\tif(myBrain~=nil) then\n" +
                "\t\t\t\t\t\t\tunits = myBrain:GetUnitsAroundPoint(categories.ALLUNITS, upos, checkDecalRange, 'Ally')\n" +
                "\t\t\t\t\t\telse\n" +
                "\t\t\t\t\t\t\tunits = GetUnitsInRect(Rect(upos[1] - checkDecalRange, upos[3] - checkDecalRange, upos[1] + checkDecalRange, upos[3] + checkDecalRange))\n" +
                "\t\t\t\t\t\tend\n" +
                "\t\t\t\t\t\tif(units~=nil and table.getn(units) > 0)then\n" +
                "\t\t\t\t\t\t\tdecal.decal:Destroy()\n" +
                "\t\t\t\t\t\t\tdecal.position = nil\n" +
                "\t\t\t\t\t\t\ttable.setn(decals, table.getn(decals) - 1)\n" +
                "\t\t\t\t\t\tend\n" +
                "\t\t\t\t\tend\n" +
                "\t\t\t\tend\n" +
                "\t\t\tend\n" +
                "\t\t\tWaitSeconds(1)\n" +
                "\t\tend\n" +
                "\tend)\n" +
                "end\n" +
                "\n" +
                "function CheckMexes()\n" +
                "\tForkThread(function()\n" +
                "\t\twhile(table.getn(mexLocations) > 0) do\n" +
                "\t\t\tfor i,mex in mexLocations do\n" +
                "\t\t\t\tif mex ~= nil then\n" +
                "\t\t\t\t\tlocal upos = mex.position\n" +
                "\t\t\t\t\tif upos ~= nil then\n" +
                "\t\t\t\t\t\tlocal units = GetUnitsInRect(Rect(upos[1] - checkRange, upos[3] - checkRange, upos[1] + checkRange, upos[3] + checkRange))\n" +
                "\t\t\t\t\t\tif(units~=nil and table.getn(units) > 0)then\n" +
                "\t\t\t\t\t\t\tCreateResourceDeposit(\"Mass\", upos[1], upos[2], upos[3], 1)\n" +
                "\t\t\t\t\t\t\tCreatePropHPR('/env/common/props/massDeposit01_prop.bp',upos[1],upos[2],upos[3],0, 0, 0)\n" +
                "\t\t\t\t\t\t\tCreateSplat(upos,0,\"/env/common/splats/mass_marker.dds\",2,2,10000,0,-1 ,0)\n" +
                "\t\t\t\t\t\t\tmex.position = nil\n" +
                "\t\t\t\t\t\t\tmex.marked = true\n" +
                "\t\t\t\t\t\t\ttable.setn(mexLocations, table.getn(mexLocations) - 1)\n" +
                "\t\t\t\t\t\tend\n" +
                "\t\t\t\t\tend\n" +
                "\t\t\t\tend\n" +
                "\t\t\tend\n" +
                "\t\t\tWaitSeconds(1)\n" +
                "\t\tend\n" +
                "\tend)\n" +
                "end\n" +
                "\n" +
                "function CheckHydros()\n" +
                "\tForkThread(function()\n" +
                "\t\twhile(table.getn(hydroLocations) > 0) do\n" +
                "\t\t\tfor i,hydro in hydroLocations do\n" +
                "\t\t\t\tif hydro ~= nil then\n" +
                "\t\t\t\t\t\tlocal upos = hydro.position\n" +
                "\t\t\t\t\t\tif upos ~= nil then\n" +
                "\t\t\t\t\t\t\tlocal units = GetUnitsInRect(Rect(upos[1] - checkRange, upos[3] - checkRange, upos[1] + checkRange, upos[3] + checkRange))\n" +
                "\t\t\t\t\t\t\tif(units~=nil and table.getn(units) > 0)then\n" +
                "\t\t\t\t\t\t\t\tCreateResourceDeposit(\"Hydrocarbon\", upos[1], upos[2], upos[3], 3)\n" +
                "\t\t\t\t\t\t\t\tCreatePropHPR('/env/common/props/hydrocarbonDeposit01_prop.bp',upos[1], upos[2], upos[3],0, 0, 0)\n" +
                "\t\t\t\t\t\t\t\tCreateSplat(upos,0,\"/env/common/splats/hydrocarbon_marker.dds\",6,6,10000,0,-1 ,0)\n" +
                "\t\t\t\t\t\t\t\thydro.position = nil\n" +
                "\t\t\t\t\t\t\t\thydro.marked = true\n" +
                "\t\t\t\t\t\t\t\ttable.setn(hydroLocations, table.getn(hydroLocations) - 1)\n" +
                "\t\t\t\t\t\t\tend\n" +
                "\t\t\t\t\t\tend\n" +
                "\t\t\t\tend\n" +
                "\t\t\tend\n" +
                "\t\t\tWaitSeconds(1)\n" +
                "\t\tend\n" +
                "\tend)\n" +
                "end\n" +
                "\n" +
                "function CheckProps()\n" +
                "\tForkThread(function()\n" +
                "\t\twhile(table.getn(propLocations) > 0) do\n" +
                "\t\t\tfor i,prop in propLocations do\n" +
                "\t\t\t\tif prop ~= nil then\n" +
                "\t\t\t\t\tlocal upos = prop.position\n" +
                "\t\t\t\t\tif upos ~= nil then\n" +
                "\t\t\t\t\t\tlocal units = GetUnitsInRect(Rect(upos[1] - checkRange, upos[3] - checkRange, upos[1] + checkRange, upos[3] + checkRange))\n" +
                "\t\t\t\t\t\tif(units~=nil and table.getn(units) > 0)then\n" +
                "\t\t\t\t\t\t\tCreatePropHPR(prop.bp,upos[1],upos[2],upos[3],prop.heading, 0, 0)\n" +
                "\t\t\t\t\t\t\tprop.position = nil\n" +
                "\t\t\t\t\t\t\tprop.marked = true\n" +
                "\t\t\t\t\t\t\ttable.setn(propLocations, table.getn(propLocations) - 1)\n" +
                "\t\t\t\t\t\tend\n" +
                "\t\t\t\t\tend\n" +
                "\t\t\t\tend\n" +
                "\t\t\tend\n" +
                "\t\t\tWaitSeconds(1)\n" +
                "\t\tend\n" +
                "\tend)\n" +
                "end\n");
        return stringBuilder.toString();
    }
}
