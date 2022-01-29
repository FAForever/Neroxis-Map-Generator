package com.faforever.neroxis.map.generator.placement;

import com.faforever.neroxis.map.AIMarker;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.vector.Vector2;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public strictfp class AIMarkerPlacer {

    public static void placeAIMarkers(BooleanMask passable, List<AIMarker> markers, String nameFormat) {
        LinkedHashSet<Vector2> coordinates = new LinkedHashSet<>(passable.getSpacedCoordinatesEqualTo(true, 32, 8));
        coordinates.addAll(passable.getDistanceField().copyAsLocalMaximums(8f, (float) passable.getSize()).getSpacedCoordinatesEqualTo(true, 16, 4));
        LinkedHashSet<Vector2> unusedCoordinates = new LinkedHashSet<>();
        coordinates.forEach(location -> {
            if (!unusedCoordinates.contains(location)) {
                if (!passable.inTeam(location, false)) {
                    unusedCoordinates.add(location);
                    return;
                }
                coordinates.forEach(location1 -> {
                    if (location != location1) {
                        if (location.getDistance(location1) < 64) {
                            LinkedHashSet<Vector2> lineCoordinates = location.getLine(location1);
                            boolean connected = !lineCoordinates.removeIf(loc -> !passable.inBounds(loc) || !passable.get(loc));
                            if (connected) {
                                unusedCoordinates.add(location1);
                            }
                        }
                    }
                });
            }
        });
        coordinates.removeAll(unusedCoordinates);
        coordinates.forEach(location -> location.roundToNearestHalfPoint());
        List<Vector2> coordinatesList = new ArrayList<>(coordinates);
        coordinates.forEach((location) -> {
            AIMarker aiMarker = new AIMarker(String.format(nameFormat, coordinatesList.indexOf(location)), location, new LinkedHashSet<>());
            markers.add(aiMarker);
            List<Vector2> symmetryPoints = passable.getSymmetryPoints(aiMarker.getPosition(), SymmetryType.SPAWN);
            symmetryPoints.forEach(symmetryPoint -> markers.add(new AIMarker(String.format(nameFormat + "s%d", coordinatesList.indexOf(location), symmetryPoints.indexOf(symmetryPoint)), symmetryPoint, new LinkedHashSet<>())));
        });
        markers.forEach(aiMarker -> markers.forEach(aiMarker1 -> {
            if (aiMarker != aiMarker1 && aiMarker.getPosition().getXZDistance(aiMarker1.getPosition()) <= 128) {
                LinkedHashSet<Vector2> lineCoordinates = aiMarker.getPosition().getXZLine(aiMarker1.getPosition());
                boolean connected = !lineCoordinates.removeIf(location -> !passable.inBounds(location) || !passable.get(location));
                if (connected) {
                    aiMarker.addNeighbor(aiMarker1.getId());
                }
            }
        }));
    }

    public static void placeAirAIMarkers(SCMap map) {
        float airMarkerSpacing = 64;
        float airMarkerConnectionDistance = (float) StrictMath.sqrt(airMarkerSpacing * airMarkerSpacing * 2) + 1;
        List<Vector2> airCoordinates = new BooleanMask(map.getSize() + 1, null, null).getSpacedCoordinates(airMarkerSpacing, (int) airMarkerSpacing / 8);
        airCoordinates.forEach((location) -> map.addAirMarker(new AIMarker(String.format("AirPN%d", airCoordinates.indexOf(location)), location.roundToNearestHalfPoint(), new LinkedHashSet<>())));
        map.getAirAIMarkers().forEach(aiMarker -> map.getAirAIMarkers().forEach(aiMarker1 -> {
            if (aiMarker != aiMarker1 && aiMarker.getPosition().getXZDistance(aiMarker1.getPosition()) < airMarkerConnectionDistance) {
                aiMarker.addNeighbor(aiMarker1.getId());
            }
        }));
    }

}
