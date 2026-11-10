package com.faforever.neroxis.exporter;

import com.faforever.neroxis.biomes.BiomeName;
import com.faforever.neroxis.biomes.Biomes;
import com.faforever.neroxis.importer.SCMapImporter;
import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.Prop;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Unit;
import com.faforever.neroxis.util.serial.biome.PropMaterials;
import com.faforever.neroxis.util.vector.Vector2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScenarioExporterTest {
    @TempDir
    Path folder;

    @Test
    public void completeTotalsAreExported() throws IOException {
        SCMap map = createMap();
        addKnownProp(map);

        ScenarioExporter.exportScenario(folder, map);

        assertTrue(readScenario().contains("  reclaim = {8.00, 80},\n"));
    }

    @Test
    public void emptyMapExportsZeroTotals() throws IOException {
        ScenarioExporter.exportScenario(folder, createMap());

        assertTrue(readScenario().contains("  reclaim = {0.00, 0},\n"));
    }

    @Test
    public void unknownPropOmitsTotalsButCompletesExport() throws IOException {
        SCMap map = createMap();
        addKnownProp(map);
        map.addProp(new Prop("/unknown_prop.bp", new Vector2(0, 0), 0, false));

        ScenarioExporter.exportScenario(folder, map);

        assertScenarioWithoutTotals();
    }

    @Test
    public void unknownWreckageOmitsTotalsButCompletesExport() throws IOException {
        SCMap map = createMap();
        addKnownProp(map);
        Army army = new Army("ARMY_17");
        Group group = new Group("WRECKAGE");
        group.addUnit(new Unit("wreck", "unknown", new Vector2(0, 0), 0));
        army.addGroup(group);
        map.addArmy(army);

        ScenarioExporter.exportScenario(folder, map);

        assertScenarioWithoutTotals();
    }

    @Test
    public void importedMapCanBeResizedAndFullyExportedWithoutPartialTotals() throws IOException {
        SCMap original = createMap();
        original.setFolderName("testmap");
        addKnownProp(original);
        original.addProp(new Prop(original.getBiome().propMaterials().rocks().getFirst().path(),
                                  new Vector2(0, 0), 0, false));
        Path input = folder.resolve("input");
        MapExporter.exportMap(input, original, false);
        SCMap imported = SCMapImporter.importSCMAP(input.resolve("testmap"));
        imported.setFolderName("testmap");
        assertTrue(imported.getBiome().propMaterials().rocks().isEmpty());
        assertTrue(imported.getReclaim().isEmpty());
        imported.changeStratumSize(32);

        Path output = folder.resolve("output");
        MapExporter.exportMap(output, imported, false);

        Path mapFolder = output.resolve("testmap");
        String scenario = Files.readString(mapFolder.resolve("testmap_scenario.lua"));
        assertFalse(scenario.contains("reclaim ="));
        assertTrue(Files.exists(mapFolder.resolve("testmap_script.lua")));
        SCMap exported = SCMapImporter.importSCMAP(mapFolder);
        assertEquals(original.getProps(), exported.getProps());
        assertEquals(32, exported.getTextureMasksLow().getWidth());
        assertEquals(32, exported.getTextureMasksHigh().getWidth());
    }

    private SCMap createMap() {
        SCMap map = new SCMap(16, Biomes.loadBiome(BiomeName.BRIMSTONE));
        map.setFilePrefix("testmap");
        return map;
    }

    private void addKnownProp(SCMap map) {
        map.addProp(new Prop(PropMaterials.OAK_AND_PINE_TREE_GROUPS.get(1).path(), new Vector2(0, 0), 0, false));
    }

    private String readScenario() throws IOException {
        return Files.readString(folder.resolve("testmap_scenario.lua"));
    }

    private void assertScenarioWithoutTotals() throws IOException {
        String scenario = readScenario();
        assertFalse(scenario.contains("reclaim ="));
        assertTrue(scenario.contains("  Configurations = {"));
        assertTrue(scenario.endsWith("}\n"));
    }
}
