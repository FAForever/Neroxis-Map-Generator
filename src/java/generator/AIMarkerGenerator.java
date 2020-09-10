package generator;

import map.AIMarker;
import map.BinaryMask;
import map.SCMap;
import util.Vector2f;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
            symmetryRegion = new BinaryMask(passable.getSize(), random.nextLong(), passable.getSymmetryHierarchy());
            symmetryRegion.fillHalf(true);
            passable.intersect(symmetryRegion);
            passableLand.intersect(symmetryRegion);
            passableWater.intersect(symmetryRegion);
        }

        float airMarkerSpacing = 64f;
        float airMarkerConnectionDistance = (float) StrictMath.sqrt(airMarkerSpacing * airMarkerSpacing * 2) + 1;
        LinkedHashSet<Vector2f> airCoordinates = passableLand.getSpacedCoordinates(airMarkerSpacing, 8);
        ArrayList<Vector2f> airCoordinatesArray = new ArrayList<>(airCoordinates);
        airCoordinates.forEach((location) -> map.addAirMarker(new AIMarker(airCoordinatesArray.indexOf(location), location, IntStream.range(0, airCoordinatesArray.size())
                .filter((ind) -> location.getDistance(airCoordinatesArray.get(ind)) < airMarkerConnectionDistance && ind != airCoordinatesArray.indexOf(location))
                .boxed().collect(Collectors.toList()))));

        float markerConnectionDistance = (float) StrictMath.sqrt(markerSpacing * markerSpacing * 2) + 1;
        LinkedHashSet<Vector2f> amphibiousCoordinates = passable.getSpacedCoordinatesEqualTo(true, markerSpacing, 4);
        if (symmetric) {
            LinkedHashSet<Vector2f> symAmphibiousCoordinates = new LinkedHashSet<>();
            amphibiousCoordinates.forEach((location) -> symAmphibiousCoordinates.add(passable.getSymmetryPoint(location)));
            amphibiousCoordinates.addAll(symAmphibiousCoordinates);
        }
        ArrayList<Vector2f> amphibiousCoordinatesArray = new ArrayList<>(amphibiousCoordinates);
        amphibiousCoordinates.forEach((location) -> map.addAmphibiousMarker(new AIMarker(amphibiousCoordinatesArray.indexOf(location), location, IntStream.range(0, amphibiousCoordinatesArray.size())
                .filter((ind) -> location.getDistance(amphibiousCoordinatesArray.get(ind)) < markerConnectionDistance && ind != amphibiousCoordinatesArray.indexOf(location))
                .boxed().collect(Collectors.toList()))));
        map.getAmphibiousAIMarkers().forEach(aiMarker -> pruneMarkerNeighbors(aiMarker, map.getAmphibiousAIMarkers(), pruneSpacing));

        LinkedHashSet<Vector2f> landCoordinates = new LinkedHashSet<>(amphibiousCoordinates);
        landCoordinates.removeIf((location) -> !passableLand.get(location));
        if (symmetric) {
            LinkedHashSet<Vector2f> symLandCoordinates = new LinkedHashSet<>();
            landCoordinates.forEach((location) -> symLandCoordinates.add(passable.getSymmetryPoint(location)));
            landCoordinates.addAll(symLandCoordinates);
        }
        ArrayList<Vector2f> landCoordinatesArray = new ArrayList<>(landCoordinates);
        landCoordinates.forEach((location) -> map.addLandMarker(new AIMarker(landCoordinatesArray.indexOf(location), location, IntStream.range(0, landCoordinatesArray.size())
                .filter((ind) -> location.getDistance(landCoordinatesArray.get(ind)) < markerConnectionDistance && ind != landCoordinatesArray.indexOf(location))
                .boxed().collect(Collectors.toList())))
        );
        map.getLandAIMarkers().forEach(aiMarker -> pruneMarkerNeighbors(aiMarker, map.getLandAIMarkers(), pruneSpacing));

        LinkedHashSet<Vector2f> navyCoordinates = new LinkedHashSet<>(amphibiousCoordinates);
        navyCoordinates.removeIf((location) -> !passableWater.get(location));
        if (symmetric) {
            LinkedHashSet<Vector2f> symNavyCoordinates = new LinkedHashSet<>();
            navyCoordinates.forEach((location) -> symNavyCoordinates.add(passable.getSymmetryPoint(location)));
            navyCoordinates.addAll(symNavyCoordinates);
        }
        ArrayList<Vector2f> navyCoordinatesArray = new ArrayList<>(navyCoordinates);
        navyCoordinates.forEach((location) -> map.addNavyMarker(new AIMarker(navyCoordinatesArray.indexOf(location), location, IntStream.range(0, navyCoordinatesArray.size())
                .filter((ind) -> location.getDistance(navyCoordinatesArray.get(ind)) < markerConnectionDistance && ind != navyCoordinatesArray.indexOf(location))
                .boxed().collect(Collectors.toList())))
        );
        map.getNavyAIMarkers().forEach(aiMarker -> pruneMarkerNeighbors(aiMarker, map.getNavyAIMarkers(), pruneSpacing));
    }

    public void pruneMarkerNeighbors(AIMarker aiMarker, List<AIMarker> aiMarkers, float spacing) {
        LinkedHashSet<Integer> validNeighbors = getFarNeighbors(aiMarker, aiMarker, aiMarkers, new LinkedHashSet<>(), spacing);
        aiMarker.getNeighbors().removeIf(id -> !validNeighbors.contains(id));
        aiMarker.getNeighbors().addAll(validNeighbors);
    }

    public LinkedHashSet<Integer> getFarNeighbors(AIMarker origMarker, AIMarker neighbor, List<AIMarker> aiMarkers, LinkedHashSet<Integer> seenIDs, float spacing) {
        LinkedHashSet<Integer> farNeighbors = new LinkedHashSet<>();
        neighbor.getNeighbors().forEach(id -> {
            seenIDs.add(id);
            AIMarker neighborNeighbor = aiMarkers.get(id);
            if (neighborNeighbor.getNeighborCount() > 0) {
                if (origMarker.getPosition().getXZDistance(neighborNeighbor.getPosition()) > spacing) {
                    farNeighbors.add(id);
                } else if (origMarker.equals(neighbor) || !seenIDs.contains(id)) {
                    neighborNeighbor.getNeighbors().removeIf(nid -> nid == neighbor.getId());
                    farNeighbors.addAll(getFarNeighbors(origMarker, neighborNeighbor, aiMarkers, seenIDs, spacing));
                    neighborNeighbor.getNeighbors().clear();
                }
            }
        });
        return farNeighbors;
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
