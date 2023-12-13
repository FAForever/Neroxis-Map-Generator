package com.faforever.neroxis.generator.cli;

import com.faforever.neroxis.cli.CLIUtils;
import com.faforever.neroxis.generator.Biome;
import com.faforever.neroxis.generator.MapGenerator;
import com.faforever.neroxis.map.Symmetry;
import lombok.Getter;
import picocli.CommandLine;

import static picocli.CommandLine.Option;
import static picocli.CommandLine.Spec;

@Getter
@SuppressWarnings("unused")
public class ParameterOptions {
    @Spec
    CommandLine.Model.CommandSpec spec;
    private Float landDensity;
    private Float plateauDensity;
    private Float mountainDensity;
    private Float rampDensity;
    private Float reclaimDensity;
    private Float mexDensity;
    private Symmetry terrainSymmetry;
    private Biome biome;

    @Option(names = "--land-density", order = 1, description = "Land density for the generated map. Min: 0 Max: 1")
    public void setLandDensity(float density) {
        this.landDensity = CLIUtils.convertDensity(density, MapGenerator.NUM_BINS, spec);
    }

    @Option(names = "--plateau-density", order = 2, description = "Plateau density for the generated map. Min: 0 Max: 1")
    public void setPlateauDensity(Float density) {
        this.plateauDensity = CLIUtils.convertDensity(density, MapGenerator.NUM_BINS, spec);
    }

    @Option(names = "--mountain-density", order = 3, description = "Mountain density for the generated map. Min: 0 Max: 1")
    public void setMountainDensity(Float density) {
        this.mountainDensity = CLIUtils.convertDensity(density, MapGenerator.NUM_BINS, spec);
    }

    @Option(names = "--ramp-density", order = 4, description = "Ramp density for the generated map. Min: 0 Max: 1")
    public void setRampDensity(Float density) {
        this.rampDensity = CLIUtils.convertDensity(density, MapGenerator.NUM_BINS, spec);
    }

    @Option(names = "--reclaim-density", order = 5, description = "Reclaim density for the generated map. Min: 0 Max: 1")
    public void setReclaimDensity(Float density) {
        this.reclaimDensity = CLIUtils.convertDensity(density, MapGenerator.NUM_BINS, spec);
    }

    @Option(names = "--mex-density", order = 6, description = "Mex density for the generated map. Min: 0 Max: 1")
    public void setMexDensity(Float density) {
        this.mexDensity = CLIUtils.convertDensity(density, MapGenerator.NUM_BINS, spec);
    }

    @Option(names = "--terrain-symmetry", order = 7, description = "Base terrain symmetry for the map. Values: ${COMPLETION-CANDIDATES}")
    public void setTerrainSymmetry(Symmetry terrainSymmetry) {
        this.terrainSymmetry = terrainSymmetry;
    }

    @Option(names = "--biome", order = 8, description = "Texture biome for the generated map")
    public void setBiome(String biome) {
        this.biome = Biome.getByValue(biome);
    }
}
