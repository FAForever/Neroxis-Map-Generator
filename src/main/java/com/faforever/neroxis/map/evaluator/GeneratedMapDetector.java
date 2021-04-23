package com.faforever.neroxis.map.evaluator;

import com.faforever.neroxis.map.DecalGroup;
import com.faforever.neroxis.map.Marker;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.importer.MapImporter;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Locale;

public class GeneratedMapDetector {

    public static void main(String[] args) throws IOException {

        Locale.setDefault(Locale.ROOT);

        SCMap map = MapImporter.importMap(Paths.get(args[0]));

        System.out.println();

        for (Marker marker : map.getBlankMarkers()) {
            if (marker.getId().contains("neroxis_map_generator")) {
                System.out.println(marker.getId());
                return;
            }
        }

        for (DecalGroup decalGroup : map.getDecalGroups()) {
            if (decalGroup.getName().contains("neroxis_map_generator")) {
                System.out.println(decalGroup.getName());
                return;
            }
        }

        System.out.println("Map is not sourced from generated map");
    }

}
