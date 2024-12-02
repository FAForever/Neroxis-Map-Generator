package com.faforever.neroxis.importer;

import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.Marker;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Spawn;
import com.faforever.neroxis.map.Unit;
import com.faforever.neroxis.util.vector.Vector3;
import com.faforever.neroxis.util.vector.Vector4;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Execution(ExecutionMode.CONCURRENT)
public class SaveImporterTest {

    @Test
    public void testSaveImport() throws Exception {
        SCMap map = new SCMap(256, null);
        SaveImporter.importSave(Path.of("src/test/resources/testmap"), map);

        assertEquals(new Vector4(1, 2, 3, 4), map.getPlayableArea());
        assertEquals(1, map.getSpawnCount());
        Spawn spawn = map.getSpawn(0);
        assertEquals("ARMY_1", spawn.getId());
        assertEquals(new Vector3(45.5f, 64f, 209.5f), spawn.getPosition());

        assertEquals(1, map.getMexCount());
        Marker mex = map.getMex(0);
        assertEquals("Mass 00", mex.getId());
        assertEquals(new Vector3(119.5f, 64f, 19.5f), mex.getPosition());
        
        assertEquals(1, map.getHydroCount());
        Marker hydro = map.getHydro(0);
        assertEquals("Hydrocarbon 00", hydro.getId());
        assertEquals(new Vector3(47.5f, 64f, 124.5f), hydro.getPosition());

        assertEquals(2, map.getArmyCount());
        Army army1 = map.getArmy("ARMY_1");
        assertNotNull(army1);
        assertEquals(1, army1.getGroupCount());
        Group initialGroup1 = army1.getGroup("INITIAL");
        assertNotNull(initialGroup1);

        Army civilianArmy = map.getArmy("NEUTRAL_CIVILIAN");
        assertNotNull(civilianArmy);
        assertEquals(1, civilianArmy.getGroupCount());
        Group initialGroupCivilian = civilianArmy.getGroup("INITIAL");
        assertNotNull(initialGroupCivilian);
        assertEquals(1, initialGroupCivilian.getUnitCount());

        Unit unit = initialGroupCivilian.getUnit(0);
        assertEquals(new Vector3(128.5f, 64f, 127.5f), unit.getPosition());
        assertEquals("xsc8001", unit.getType());
    }

}
