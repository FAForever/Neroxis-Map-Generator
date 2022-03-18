package com.faforever.neroxis.exporter;

import com.faforever.neroxis.map.Marker;
import com.faforever.neroxis.map.Prop;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.util.vector.Vector3;

public strictfp class ScriptGenerator {

    public static void generateScript(SCMap map) {
        if (map.isUnexplored()) {
            map.setScript(generateUnexploredScript(map));
        } else {
            map.setScript(generateDefaultScript());
        }
    }

    private static String generateDefaultScript() {
        return """
                local ScenarioUtils = import('/lua/sim/ScenarioUtilities.lua')
                local ScenarioFramework = import('/lua/ScenarioFramework.lua')
                function OnPopulate()
                ScenarioUtils.InitializeArmies()
                ScenarioFramework.SetPlayableArea('AREA_1' , false)
                end
                function OnStart(self)
                end""";
    }

    private static String generateUnexploredScript(SCMap map) {
        int mapPlayableSize = (int) (map.getPlayableArea().getW() - map.getPlayableArea().getX());
        int decalSize = mapPlayableSize * mapPlayableSize / 8192;
        int checkDecalRange = decalSize / 2 + 24;
        int checkResourceRange = decalSize / 2 + 32;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("local mexLocations = {");
        for (Marker mex : map.getMexes()) {
            Vector3 v = mex.getPosition();
            stringBuilder.append(String.format("{position = VECTOR3( %s )},", v.toString()));
        }
        stringBuilder.append("}\n");
        stringBuilder.append("local hydroLocations = {");
        for (Marker hydro : map.getHydros()) {
            Vector3 v = hydro.getPosition();
            stringBuilder.append(String.format("{position = VECTOR3( %s )},", v.toString()));
        }
        stringBuilder.append("}\n");
        stringBuilder.append("local propLocations = {");
        for (Prop prop : map.getProps()) {
            Vector3 v = prop.getPosition();
            stringBuilder.append(String.format("{position = VECTOR3( %s ), bp = '%s', heading = %f},", v.toString(), prop.getPath(), prop.getRotation()));
        }
        stringBuilder.append("}\n");
        stringBuilder.append(String.format("""
                local ScenarioFramework = import('/lua/ScenarioFramework.lua')
                local ScenarioUtils = import('/lua/sim/ScenarioUtilities.lua')
                local myBrain = nil
                local decals = {}
                local decalSpacing = %1$d
                local decalSize = %1$d
                local checkDecalRange = %2$d
                local checkRange = %3$d
                """, decalSize, checkDecalRange, checkResourceRange));
        stringBuilder.append(String.format("""
                function OnPopulate()
                    ScenarioUtils.InitializeArmies()
                end

                function OnStart(self)
                    ScenarioFramework.SetPlayableArea(Rect( %1$d - 4, %1$d - 4, %1$d + 4, %1$d + 4 ) , false)
                    AddFogDecals()
                    ScenarioFramework.CreateTimerTrigger(MapExpand, 1, true)
                    local player = GetFocusArmy()
                    myBrain = nil
                    for _, v in ArmyBrains do
                        if v:GetArmyIndex() == player then
                            myBrain = v
                            break
                        end
                    end
                end

                """, mapPlayableSize / 2));
        stringBuilder.append("""
                function MapExpand()
                    ScenarioFramework.SetPlayableArea('AREA_1' , false)
                    CheckDecals()
                    CheckMexes()
                    CheckHydros()
                    CheckProps()
                end

                function AddFogDecals()
                    local count = 0
                    local decal = CreateDecal({0, 0, 0}, 0, "/env/Common/decals/NoSpec_spec.dds", "", "Water Albedo", 1, 1, 10000, 0, -1, 0)
                    local position = {0,0,0}
                    local decalInfo = {decal = decal, position = position}
                    count = count + 1
                    table.insert(decals, decalInfo)
                    for i=decalSize / 2,ScenarioInfo.size[1],decalSpacing do
                        for j=decalSize / 2,ScenarioInfo.size[2],decalSpacing do
                            local decal = CreateDecal({i, 0, j}, 0, "/env/Common/decals/NoSpec_spec.dds", "", "Water Albedo", decalSize * 1.125, decalSize * 1.125, 10000, 0, -1, -1)
                            local position = {i,0,j}
                            local decalInfo = {decal = decal, position = position}
                            count = count + 1
                            table.insert(decals, decalInfo)
                        end
                    end
                    table.setn(decals, count + 1)
                end

                function CheckDecals()
                    ForkThread(function()
                        while(table.getn(decals) > 1) do
                            for i,decal in decals do
                                if decal ~= nil then
                                    local upos = decal.position
                                    if upos ~= nil then
                                        local units = nil
                                        if(myBrain~=nil) then
                                            units = myBrain:GetUnitsAroundPoint(categories.ALLUNITS, upos, checkDecalRange, 'Ally')
                                        else
                                            units = GetUnitsInRect(Rect(upos[1] - checkDecalRange, upos[3] - checkDecalRange, upos[1] + checkDecalRange, upos[3] + checkDecalRange))
                                        end
                                        if(units~=nil and table.getn(units) > 0)then
                                            decal.decal:Destroy()
                                            decal.position = nil
                                            table.setn(decals, table.getn(decals) - 1)
                                        end
                                    end
                                end
                            end
                            WaitSeconds(1)
                        end
                    end)
                end

                function CheckMexes()
                    ForkThread(function()
                        while(table.getn(mexLocations) > 0) do
                            for i,mex in mexLocations do
                                if mex ~= nil then
                                    local upos = mex.position
                                    if upos ~= nil then
                                        local units = GetUnitsInRect(Rect(upos[1] - checkRange, upos[3] - checkRange, upos[1] + checkRange, upos[3] + checkRange))
                                        if(units~=nil and table.getn(units) > 0)then
                                            CreateResourceDeposit("Mass", upos[1], upos[2], upos[3], 1)
                                            CreatePropHPR('/env/common/props/massDeposit01_prop.bp',upos[1],upos[2],upos[3],0, 0, 0)
                                            CreateSplat(upos,0,"/env/common/splats/mass_marker.dds",2,2,10000,0,-1 ,0)
                                            mex.position = nil
                                            table.setn(mexLocations, table.getn(mexLocations) - 1)
                                            local mexTable = {
                                                                   ['size'] = FLOAT( 1.000000 ),
                                                                   ['amount'] = FLOAT( 100.000000 ),
                                                                   ['editorIcon'] = STRING( '/textures/editor/marker_mass.bmp' ),
                                                                   ['color'] = STRING( 'ff808080' ),
                                                                   ['resource'] = BOOLEAN( true ),
                                                                   ['type'] = STRING( 'Mass' ),
                                                                   ['prop'] = STRING( '/env/common/props/markers/M_Mass_prop.bp' ),
                                                                   ['orientation'] = VECTOR3( 0, 0, 0 ),
                                                                   ['position'] = VECTOR3( upos[1], upos[2], upos[3] ),
                                                               }
                                            table.insert(Scenario.MasterChain._MASTERCHAIN_.Markers, mexTable)
                                        end
                                    end
                                end
                            end
                            WaitSeconds(1)
                        end
                    end)
                end

                function CheckHydros()
                    ForkThread(function()
                        while(table.getn(hydroLocations) > 0) do
                            for i,hydro in hydroLocations do
                                if hydro ~= nil then
                                        local upos = hydro.position
                                        if upos ~= nil then
                                            local units = GetUnitsInRect(Rect(upos[1] - checkRange, upos[3] - checkRange, upos[1] + checkRange, upos[3] + checkRange))
                                            if(units~=nil and table.getn(units) > 0)then
                                                CreateResourceDeposit("Hydrocarbon", upos[1], upos[2], upos[3], 3)
                                                CreatePropHPR('/env/common/props/hydrocarbonDeposit01_prop.bp',upos[1], upos[2], upos[3],0, 0, 0)
                                                CreateSplat(upos,0,"/env/common/splats/hydrocarbon_marker.dds",6,6,10000,0,-1 ,0)
                                                hydro.position = nil
                                                table.setn(hydroLocations, table.getn(hydroLocations) - 1)
                                                local hydroTable = {
                                                                       ['size'] = FLOAT( 3.000000 ),
                                                                       ['amount'] = FLOAT( 100.000000 ),
                                                                       ['color'] = STRING( 'ff007f00' ),
                                                                       ['resource'] = BOOLEAN( true ),
                                                                       ['type'] = STRING( 'Hydrocarbon' ),
                                                                       ['prop'] = STRING( '/env/common/props/markers/M_Hydrocarbon_prop.bp' ),
                                                                       ['orientation'] = VECTOR3( 0, 0, 0 ),
                                                                       ['position'] = VECTOR3( upos[1], upos[2], upos[3] ),
                                                                   }
                                                table.insert(Scenario.MasterChain._MASTERCHAIN_.Markers, hydroTable)
                                            end
                                        end
                                end
                            end
                            WaitSeconds(1)
                        end
                    end)
                end

                function CheckProps()
                    ForkThread(function()
                        while(table.getn(propLocations) > 0) do
                            for i,prop in propLocations do
                                if prop ~= nil then
                                    local upos = prop.position
                                    if upos ~= nil then
                                        local units = GetUnitsInRect(Rect(upos[1] - checkRange, upos[3] - checkRange, upos[1] + checkRange, upos[3] + checkRange))
                                        if(units~=nil and table.getn(units) > 0)then
                                            CreatePropHPR(prop.bp,upos[1],upos[2],upos[3],prop.heading, 0, 0)
                                            prop.position = nil
                                            table.setn(propLocations, table.getn(propLocations) - 1)
                                        end
                                    end
                                end
                            end
                            WaitSeconds(1)
                        end
                    end)
                end
                """);
        return stringBuilder.toString();
    }
}
