package com.faforever.neroxis.generator;

import com.faforever.neroxis.biomes.BiomeName;
import com.faforever.neroxis.generator.util.serial.PropStyle;
import com.faforever.neroxis.generator.util.serial.ResourceStyle;
import com.faforever.neroxis.generator.util.serial.TerrainStyle;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.Pipeline;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.List;

record GenerationResults(
        SCMap map,
        SymmetrySettings symmetrySettings,
        TerrainStyle terrainStyle,
        PropStyle propStyle,
        BiomeName biomeName,
        ResourceStyle resourceStyle,
        List<Pipeline.Entry> terrainPipelineEntries,
        List<Pipeline.Entry> placementPipelineEntries
) {
    public String settingsToString() {
        return """
               Symmetry Settings: %s
               Biome: %s
               TerrainStyle: %s
               ResourceStyle: %s
               PropStyle: %s
               """.formatted(symmetrySettings, biomeName, terrainStyle, resourceStyle, propStyle);
    }

    public void writePipelines(OutputStream out) throws IOException {
        try {
            for (Pipeline.Entry entry : terrainPipelineEntries) {
                out.write(entry.getResult().toHash().getBytes(StandardCharsets.UTF_8));
                out.write("\n".getBytes(StandardCharsets.UTF_8));
            }
            for (Pipeline.Entry entry : placementPipelineEntries) {
                out.write(entry.getResult().toHash().getBytes(StandardCharsets.UTF_8));
                out.write("\n".getBytes(StandardCharsets.UTF_8));
            }
        } catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException(exception);
        }
        out.flush();
    }

}
