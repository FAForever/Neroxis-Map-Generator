package com.faforever.neroxis.importer;

import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Spawn;
import com.faforever.neroxis.util.vector.Vector2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.CONCURRENT)
public class ScenarioImporterTest {

    @Test
    public void testScenarioImport() throws Exception {
        SCMap map = new SCMap(512, null);
        map.addSpawn(new Spawn("1", new Vector2(0, 0), new Vector2(0, 0), 1));
        map.addSpawn(new Spawn("2", new Vector2(0, 0), new Vector2(0, 0), 1));
        ScenarioImporter.importScenario(Path.of("src/test/resources/testmap"), map);

        assertEquals("map name", map.getName());
        assertEquals("map description", map.getDescription());
        assertEquals(40f, map.getNoRushRadius());
        assertEquals(new Vector2(10, 5), map.getSpawn(0).getNoRushOffset());
    }

}
