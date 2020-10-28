package generator;

import map.AIMarker;
import map.BinaryMask;
import map.SCMap;
import util.Vector2f;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Random;

import static util.Placement.placeOnHeightmap;

public strictfp class AIMarkerGenerator {
    private final SCMap map;
    private final Random random;

    public AIMarkerGenerator(SCMap map, long seed) {
        this.map = map;
        random = new Random(seed);
    }

    public void generateAIMarkers(BinaryMask passable, BinaryMask passableLand, BinaryMask passableWater, float markerSpacing, float pruneSpacing) {
        generateAIMarkers(passable, passableLand, passableWater, markerSpacing, pruneSpacing, true);
    }

    public void generateAIMarkers(BinaryMask passable, BinaryMask passableLand, BinaryMask passableWater, float markerSpacing, float pruneSpacing, boolean symmetric) {
        BinaryMask symmetryRegion;
        if (symmetric) {
            symmetryRegion = new BinaryMask(passable.getSize(), random.nextLong(), passable.getSymmetrySettings());
            symmetryRegion.fillHalf(true);
            passable.intersect(symmetryRegion);
            passableLand.intersect(symmetryRegion);
            passableWater.intersect(symmetryRegion);
        }

        float airMarkerSpacing = 64f;
        float airMarkerConnectionDistance = (float) StrictMath.sqrt(airMarkerSpacing * airMarkerSpacing * 2) + 1;
        LinkedHashSet<Vector2f> airCoordinates = passableLand.getSpacedCoordinates(airMarkerSpacing, 8);
        airCoordinates.forEach((location) -> location.add(.5f, .5f));
        ArrayList<Vector2f> airCoordinatesArray = new ArrayList<>(airCoordinates);
        airCoordinates.forEach((location) -> map.addAirMarker(new AIMarker(String.format("AirPN%d", airCoordinatesArray.indexOf(location)), location, new LinkedHashSet<>())));
        map.getAirAIMarkers().forEach(aiMarker -> map.getAirAIMarkers().forEach(aiMarker1 -> {
            if (aiMarker != aiMarker1 && aiMarker.getPosition().getXZDistance(aiMarker1.getPosition()) < airMarkerConnectionDistance) {
                aiMarker.addNeighbor(aiMarker1.getId());
            }
        }));

        float markerConnectionDistance = (float) StrictMath.sqrt(markerSpacing * markerSpacing * 2) + 1;
        LinkedHashSet<Vector2f> amphibiousCoordinates = passable.getSpacedCoordinatesEqualTo(true, markerSpacing, 4);
        amphibiousCoordinates.forEach((location) -> location.add(.5f, .5f));
        if (symmetric) {
            LinkedHashSet<Vector2f> symAmphibiousCoordinates = new LinkedHashSet<>();
            amphibiousCoordinates.forEach((location) -> symAmphibiousCoordinates.add(passable.getSymmetryPoint(location)));
            amphibiousCoordinates.addAll(symAmphibiousCoordinates);
        }
        ArrayList<Vector2f> amphibiousCoordinatesArray = new ArrayList<>(amphibiousCoordinates);
        amphibiousCoordinates.forEach((location) -> map.addAmphibiousMarker(new AIMarker(String.format("AmphPN%d", amphibiousCoordinatesArray.indexOf(location)), location, new LinkedHashSet<>())));
        map.getAmphibiousAIMarkers().forEach(aiMarker -> map.getAmphibiousAIMarkers().forEach(aiMarker1 -> {
            if (aiMarker != aiMarker1 && aiMarker.getPosition().getXZDistance(aiMarker1.getPosition()) < markerConnectionDistance) {
                aiMarker.addNeighbor(aiMarker1.getId());
            }
        }));
        //TODO Fix Marker Pruning
//        map.getAmphibiousAIMarkers().forEach(aiMarker -> pruneMarkerNeighbors(aiMarker, map.getAmphibiousAIMarkers(), pruneSpacing));

        LinkedHashSet<Vector2f> landCoordinates = new LinkedHashSet<>(amphibiousCoordinates);
        landCoordinates.removeIf((location) -> !passableLand.get(location));
        if (symmetric) {
            LinkedHashSet<Vector2f> symLandCoordinates = new LinkedHashSet<>();
            landCoordinates.forEach((location) -> symLandCoordinates.add(passable.getSymmetryPoint(location)));
            landCoordinates.addAll(symLandCoordinates);
        }
        ArrayList<Vector2f> landCoordinatesArray = new ArrayList<>(landCoordinates);
        landCoordinates.forEach((location) -> map.addLandMarker(new AIMarker(String.format("LandPN%d", landCoordinatesArray.indexOf(location)), location, new LinkedHashSet<>())));
        map.getLandAIMarkers().forEach(aiMarker -> map.getLandAIMarkers().forEach(aiMarker1 -> {
            if (aiMarker != aiMarker1 && aiMarker.getPosition().getXZDistance(aiMarker1.getPosition()) < markerConnectionDistance) {
                aiMarker.addNeighbor(aiMarker1.getId());
            }
        }));
        //TODO Fix Marker Pruning
//        map.getLandAIMarkers().forEach(aiMarker -> pruneMarkerNeighbors(aiMarker, map.getLandAIMarkers(), pruneSpacing));

        LinkedHashSet<Vector2f> navyCoordinates = new LinkedHashSet<>(amphibiousCoordinates);
        navyCoordinates.removeIf((location) -> !passableWater.get(location));
        if (symmetric) {
            LinkedHashSet<Vector2f> symNavyCoordinates = new LinkedHashSet<>();
            navyCoordinates.forEach((location) -> symNavyCoordinates.add(passable.getSymmetryPoint(location)));
            navyCoordinates.addAll(symNavyCoordinates);
        }
        ArrayList<Vector2f> navyCoordinatesArray = new ArrayList<>(navyCoordinates);
        navyCoordinates.forEach((location) -> map.addNavyMarker(new AIMarker(String.format("NavyPN%d", navyCoordinatesArray.indexOf(location)), location, new LinkedHashSet<>())));
        map.getNavyAIMarkers().forEach(aiMarker -> map.getNavyAIMarkers().forEach(aiMarker1 -> {
            if (aiMarker != aiMarker1 && aiMarker.getPosition().getXZDistance(aiMarker1.getPosition()) < markerConnectionDistance) {
                aiMarker.addNeighbor(aiMarker1.getId());
            }
        }));
        //TODO Fix Marker Pruning
//        map.getNavyAIMarkers().forEach(aiMarker -> pruneMarkerNeighbors(aiMarker, map.getNavyAIMarkers(), pruneSpacing));
    }

    public void setMarkerHeights() {
        for (int i = 0; i < map.getAirMarkerCount(); i++) {
            map.getAirMarker(i).setPosition(placeOnHeightmap(map, map.getAirMarker(i).getPosition()));
        }
        for (int i = 0; i < map.getLandMarkerCount(); i++) {
            map.getLandMarker(i).setPosition(placeOnHeightmap(map, map.getLandMarker(i).getPosition()));
        }
        for (int i = 0; i < map.getAmphibiousMarkerCount(); i++) {
            map.getAmphibiousMarker(i).setPosition(placeOnHeightmap(map, map.getAmphibiousMarker(i).getPosition()));
        }
        for (int i = 0; i < map.getNavyMarkerCount(); i++) {
            map.getNavyMarker(i).setPosition(placeOnHeightmap(map, map.getNavyMarker(i).getPosition()));
        }
        for (int i = 0; i < map.getLargeExpansionMarkerCount(); i++) {
            map.getLargeExpansionMarker(i).setPosition(placeOnHeightmap(map, map.getLargeExpansionMarker(i).getPosition()));
        }
        for (int i = 0; i < map.getExpansionMarkerCount(); i++) {
            map.getExpansionMarker(i).setPosition(placeOnHeightmap(map, map.getExpansionMarker(i).getPosition()));
        }
    }
}
