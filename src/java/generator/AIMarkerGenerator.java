package generator;

import map.*;
import util.Vector2f;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Random;

import static util.Placement.placeOnHeightmap;

public strictfp class AIMarkerGenerator {
    private final SCMap map;
    private final Random random;

    public AIMarkerGenerator(SCMap map, long seed) {
        this.map = map;
        random = new Random(seed);
    }

    public void generateAIMarkers(BinaryMask passable, ArrayList<AIMarker> markerArrayList, String nameFormat) {
        LinkedHashSet<Vector2f> coordinates = new LinkedHashSet<>(passable.getSpacedCoordinatesEqualTo(true, 32, 8));
        coordinates.addAll(passable.getDistanceField().getLocalMaximums(8, passable.getSize()).getSpacedCoordinatesEqualTo(true, 16, 4));
        LinkedHashSet<Vector2f> unusedCoordinates = new LinkedHashSet<>();
        coordinates.forEach(location -> {
            if (!unusedCoordinates.contains(location)) {
                if (!passable.inTeam(location, false)) {
                    unusedCoordinates.add(location);
                    return;
                }
                coordinates.forEach(location1 -> {
                    if (location != location1) {
                        if (location.getDistance(location1) < 64) {
                            LinkedHashSet<Vector2f> lineCoordinates = location.getLine(location1);
                            boolean connected = !lineCoordinates.removeIf(loc -> !passable.getValueAt(loc));
                            if (connected) {
                                unusedCoordinates.add(location1);
                            }
                        }
                    }
                });
            }
        });
        coordinates.removeAll(unusedCoordinates);
        coordinates.forEach(location -> location.add(.5f, .5f));
        ArrayList<Vector2f> coordinatesArray = new ArrayList<>(coordinates);
        coordinates.forEach((location) -> {
            AIMarker aiMarker = new AIMarker(String.format(nameFormat, coordinatesArray.indexOf(location)), location, new LinkedHashSet<>());
            markerArrayList.add(aiMarker);
            ArrayList<SymmetryPoint> symmetryPoints = passable.getSymmetryPoints(aiMarker.getPosition(), SymmetryType.SPAWN);
            symmetryPoints.forEach(symmetryPoint -> markerArrayList.add(new AIMarker(String.format(nameFormat + "s%d", coordinatesArray.indexOf(location), symmetryPoints.indexOf(symmetryPoint)), symmetryPoint.getLocation(), new LinkedHashSet<>())));
        });
        markerArrayList.forEach(aiMarker -> markerArrayList.forEach(aiMarker1 -> {
            if (aiMarker != aiMarker1 && aiMarker.getPosition().getXZDistance(aiMarker1.getPosition()) <= 128) {
                LinkedHashSet<Vector2f> lineCoordinates = aiMarker.getPosition().getXZLine(aiMarker1.getPosition());
                boolean connected = !lineCoordinates.removeIf(location -> !passable.getValueAt(location));
                if (connected) {
                    aiMarker.addNeighbor(aiMarker1.getId());
                }
            }
        }));
    }

    public void generateAirAIMarkers() {
        float airMarkerSpacing = 64;
        float airMarkerConnectionDistance = (float) StrictMath.sqrt(airMarkerSpacing * airMarkerSpacing * 2) + 1;
        LinkedList<Vector2f> airCoordinates = new BinaryMask(map.getSize() + 1, null, null).getSpacedCoordinates(airMarkerSpacing, (int) airMarkerSpacing / 8);
        airCoordinates.forEach((location) -> location.add(.5f, .5f));
        ArrayList<Vector2f> airCoordinatesArray = new ArrayList<>(airCoordinates);
        airCoordinates.forEach((location) -> map.addAirMarker(new AIMarker(String.format("AirPN%d", airCoordinatesArray.indexOf(location)), location, new LinkedHashSet<>())));
        map.getAirAIMarkers().forEach(aiMarker -> map.getAirAIMarkers().forEach(aiMarker1 -> {
            if (aiMarker != aiMarker1 && aiMarker.getPosition().getXZDistance(aiMarker1.getPosition()) < airMarkerConnectionDistance) {
                aiMarker.addNeighbor(aiMarker1.getId());
            }
        }));
    }

    public void setMarkerHeights() {
        for (AIMarker aiMarker : map.getAirAIMarkers()) {
            aiMarker.setPosition(placeOnHeightmap(map, aiMarker.getPosition()));
        }
        for (AIMarker aiMarker : map.getLandAIMarkers()) {
            aiMarker.setPosition(placeOnHeightmap(map, aiMarker.getPosition()));
        }
        for (AIMarker aiMarker : map.getAmphibiousAIMarkers()) {
            aiMarker.setPosition(placeOnHeightmap(map, aiMarker.getPosition()));
        }
        for (AIMarker aiMarker : map.getNavyAIMarkers()) {
            aiMarker.setPosition(placeOnHeightmap(map, aiMarker.getPosition()));
        }
        for (AIMarker aiMarker : map.getLargeExpansionAIMarkers()) {
            aiMarker.setPosition(placeOnHeightmap(map, aiMarker.getPosition()));
        }
        for (AIMarker aiMarker : map.getExpansionAIMarkers()) {
            aiMarker.setPosition(placeOnHeightmap(map, aiMarker.getPosition()));
        }
    }
}
