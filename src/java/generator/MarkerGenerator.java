package generator;

import map.AIMarker;
import map.BinaryMask;
import map.SCMap;
import map.Symmetry;
import util.Vector2f;
import util.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static util.Placement.placeOnHeightmap;

public strictfp class MarkerGenerator {
    private final SCMap map;
    private final Random random;
    private final int mexSpacing = 32;
    private final int spawnSize = 32;

    public MarkerGenerator(SCMap map, long seed) {
        this.map = map;
        random = new Random(seed);
    }


    public BinaryMask[] generateSpawns(float separation, Symmetry symmetry, float plateauDensity) {
        map.getLargeExpansionAIMarkers().clear();
        BinaryMask spawnable = new BinaryMask(map.getSize() + 1, random.nextLong(), symmetry);
        BinaryMask spawnLandMask = new BinaryMask(map.getSize() + 1, random.nextLong(), spawnable.getSymmetryHierarchy());
        BinaryMask spawnPlateauMask = new BinaryMask(map.getSize() + 1, random.nextLong(), spawnable.getSymmetryHierarchy());
        if (map.getSpawns().length == 2 && (symmetry == Symmetry.POINT || symmetry == Symmetry.DIAG || symmetry == Symmetry.QUAD)) {
            spawnable.getSymmetryHierarchy().setSpawnSymmetry(Symmetry.POINT);
        }
        spawnable.fillHalf(true).fillSides(map.getSize() / map.getSpawns().length * 3 / 2, false).fillCenter(map.getSize() * 5 / 8, false).trimEdge(map.getSize() / 16);
        Vector2f location = spawnable.getRandomPosition();
        Vector2f symLocation;
        for (int i = 0; i < map.getSpawns().length; i += 2) {
            if (location == null) {
                if (separation - 4 >= 10) {
                    return generateSpawns(separation - 8, symmetry, plateauDensity);
                } else {
                    return null;
                }
            }
            symLocation = spawnable.getSymmetryPoint(location);
            spawnable.fillCircle(location, separation, false);
            spawnable.fillCircle(symLocation, separation, false);

            if (spawnable.getSymmetryHierarchy().getSpawnSymmetry() == Symmetry.POINT) {
                spawnable.fillCircle(symLocation, map.getSize() * 5 / 8f, false);
            }

            spawnLandMask.fillCircle(location, spawnSize, true);
            spawnLandMask.fillCircle(symLocation, spawnSize, true);

            if (random.nextFloat() < plateauDensity) {
                boolean valid = true;
                for (int j = 0; j < i; j += 2) {
                    if (!spawnPlateauMask.get(map.getSpawns()[j]) && map.getSpawns()[j].getXZDistance(location) < spawnSize * 4) {
                        valid = false;
                        break;
                    }
                }
                if (valid) {
                    spawnPlateauMask.fillCircle(location, spawnSize, true);
                    spawnPlateauMask.fillCircle(symLocation, spawnSize, true);
                }
            } else {
                boolean valid = false;
                for (int j = 0; j < i; j += 2) {
                    if (spawnPlateauMask.get(map.getSpawns()[j]) && map.getSpawns()[j].getXZDistance(location) < spawnSize * 4) {
                        valid = true;
                        break;
                    }
                }
                if (valid) {
                    spawnPlateauMask.fillCircle(location, spawnSize, true);
                    spawnPlateauMask.fillCircle(symLocation, spawnSize, true);
                }
            }
            map.getSpawns()[i] = new Vector3f(location);
            map.getSpawns()[i + 1] = new Vector3f(symLocation);
            map.addLargeExpansionMarker(new AIMarker(location, null));
            map.addLargeExpansionMarker(new AIMarker(symLocation, null));
            location = spawnable.getRandomPosition();
        }
        return new BinaryMask[]{spawnLandMask, spawnPlateauMask};
    }

    public void generateMexes(BinaryMask spawnable, BinaryMask spawnablePlateau, BinaryMask spawnableWater) {
        BinaryMask spawnableLand = new BinaryMask(spawnable, random.nextLong());
        spawnable.fillHalf(false);
        float spawnDensity = (float) spawnable.getCount() / map.getSize() / map.getSize() * 2;
        int mexSpawnDistance = (int) StrictMath.max(StrictMath.min(spawnDensity * map.getSize() / 6, spawnSize * 2.5), spawnSize);
        BinaryMask spawnableNoSpawns = new BinaryMask(spawnable, random.nextLong());
        int spawnCount = map.getSpawns().length;
        int totalMexCount = map.getMexes().length;
        int numBaseMexes = (random.nextInt(2) + 3) * 2;
        int spawnMexCount = numBaseMexes * spawnCount;
        int nonSpawnMexCount = totalMexCount - spawnMexCount;
        int numNearMexes = random.nextInt(nonSpawnMexCount / 24 + 1) * 2;
        int iMex = 0;
        for (int i = 0; i < map.getSpawns().length; i += 2) {
            BinaryMask baseMexes = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetryHierarchy());
            baseMexes.fillCircle(map.getSpawns()[i + 1], 20, true).fillCircle(map.getSpawns()[i + 1], 5, false).intersect(spawnable);
            for (int j = 0; j < numBaseMexes; j += 2) {
                Vector2f location = baseMexes.getRandomPosition();
                if (location == null) {
                    break;
                }
                Vector2f symLocation = baseMexes.getSymmetryPoint(location);
                map.getMexes()[iMex] = new Vector3f(location);
                map.getMexes()[iMex + 1] = new Vector3f(symLocation);
                baseMexes.fillCircle(location, 10, false);
                spawnable.fillCircle(location, mexSpacing, false);
                iMex += 2;
            }
            BinaryMask nearMexes = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetryHierarchy());
            nearMexes.fillCircle(map.getSpawns()[i + 1], spawnSize * 5, true).fillCircle(map.getSpawns()[i + 1], spawnSize * 2, false).intersect(spawnable);
            for (int j = 0; j < map.getSpawns().length; j += 2) {
                nearMexes.fillCircle(map.getSpawns()[j + 1], spawnSize, false);
            }
            for (int j = 0; j < numNearMexes; j += 2) {
                Vector2f location = nearMexes.getRandomPosition();
                if (location == null) {
                    break;
                }
                Vector2f symLocation = nearMexes.getSymmetryPoint(location);
                map.getMexes()[iMex] = new Vector3f(location);
                map.getMexes()[iMex + 1] = new Vector3f(symLocation);
                nearMexes.fillCircle(location, mexSpacing, false);
                spawnable.fillCircle(location, mexSpacing, false);
                iMex += 2;
            }
        }
        for (int i = 0; i < map.getSpawns().length; i += 2) {
            spawnable.fillCircle(map.getSpawns()[i + 1], 24, false);
            spawnableNoSpawns.fillCircle(map.getSpawns()[i + 1], mexSpawnDistance, false);
        }
        int numMexesLeft;
        int actualExpMexCount;
        int baseMexCount = iMex;
        int nonBaseMexCount = totalMexCount - baseMexCount;

        if (nonBaseMexCount / 2 > 10) {
            int possibleExpMexCount = (random.nextInt(nonBaseMexCount / 2 / spawnCount) + nonBaseMexCount / 2 / spawnCount) * 2;
            actualExpMexCount = generateMexExpansions(spawnable, baseMexCount, possibleExpMexCount);
            numMexesLeft = nonBaseMexCount - actualExpMexCount;
        } else {
            actualExpMexCount = 0;
            numMexesLeft = nonBaseMexCount;
        }

        spawnableNoSpawns.intersect(spawnable);
        spawnablePlateau.intersect(spawnableNoSpawns);
        spawnableLand.intersect(spawnableNoSpawns);
        spawnableLand.minus(spawnablePlateau);

        float plateauDensity = (float) spawnablePlateau.getCount() / spawnableNoSpawns.getCount();
        int plateauMexCount = (int) (plateauDensity * numMexesLeft / 2) * 2;

        for (int i = 0; i < plateauMexCount; i += 2) {
            int ind = i + baseMexCount + actualExpMexCount;
            Vector2f mexLocation = spawnablePlateau.getRandomPosition();

            if (mexLocation == null) {
                break;
            }

            numMexesLeft -= 2;
            Vector2f mexSymLocation = spawnablePlateau.getSymmetryPoint(mexLocation);

            map.getMexes()[ind] = new Vector3f(mexLocation);
            map.getMexes()[ind + 1] = new Vector3f(mexSymLocation);

            spawnablePlateau.fillCircle(mexLocation, mexSpacing, false);
        }

        int numLandMexes = numMexesLeft;
        for (int i = 0; i < numLandMexes; i += 2) {
            int ind = i + map.getMexes().length - numLandMexes;
            Vector2f mexLocation = spawnableLand.getRandomPosition();

            if (mexLocation == null) {
                break;
            }
            numMexesLeft -= 2;

            Vector2f mexSymLocation = spawnableLand.getSymmetryPoint(mexLocation);

            map.getMexes()[ind] = new Vector3f(mexLocation);
            map.getMexes()[ind + 1] = new Vector3f(mexSymLocation);

            spawnableLand.fillCircle(mexLocation, mexSpacing, false);
        }
        spawnable.intersect(spawnableLand.combine(spawnablePlateau));

        int numNearSpawnMexes = numMexesLeft;
        for (int i = 0; i < numNearSpawnMexes; i += 2) {
            int ind = i + map.getMexes().length - numLandMexes;
            Vector2f mexLocation = spawnable.getRandomPosition();

            if (mexLocation == null) {
                break;
            }
            numMexesLeft -= 2;

            Vector2f mexSymLocation = spawnable.getSymmetryPoint(mexLocation);

            map.getMexes()[ind] = new Vector3f(mexLocation);
            map.getMexes()[ind + 1] = new Vector3f(mexSymLocation);

            spawnable.fillCircle(mexLocation, mexSpacing, false);
            spawnable.fillCircle(mexSymLocation, mexSpacing, false);
        }

        for (int i = 0; i < numMexesLeft; i += 2) {
            int ind = i + map.getMexes().length - numMexesLeft;
            Vector2f mexLocation = spawnableWater.getRandomPosition();

            if (mexLocation == null) {
                break;
            }

            Vector2f mexSymLocation = spawnableWater.getSymmetryPoint(mexLocation);

            map.getMexes()[ind] = new Vector3f(mexLocation);
            map.getMexes()[ind + 1] = new Vector3f(mexSymLocation);

            spawnableWater.fillCircle(mexLocation, mexSpacing, false);
            spawnableWater.fillCircle(mexSymLocation, mexSpacing, false);
        }
    }

    public int generateMexExpansions(BinaryMask spawnable, int baseMexCount, int possibleExpMexCount) {
        Vector2f expLocation;
        Vector2f mexLocation;
        Vector2f mexSymLocation;
        int actualExpMexCount = possibleExpMexCount;
        int expMexCount;
        int expMexCountLeft = possibleExpMexCount;
        int iMex = baseMexCount;
        int expMexSpacing = 10;
        int expSize = 10;
        int expSpacing = 96;

        BinaryMask spawnableCopy = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetryHierarchy());
        BinaryMask expansion = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetryHierarchy());

        spawnableCopy.fillCircle(map.getSize() / 2f, map.getSize() / 2f, map.getSize() / 2f, true).fillCenter(64, false).intersect(spawnable);

        for (int i = 0; i < map.getSpawns().length; i++) {
            spawnableCopy.fillCircle(map.getSpawns()[i].x, map.getSpawns()[i].z, map.getSize() / 4f, false);
        }

        while (expMexCountLeft > 1) {
            expLocation = spawnableCopy.getRandomPosition();

            while (expLocation != null && !isMexExpValid(expLocation, expSize, .5f, spawnable)) {
                spawnableCopy.fillRect(expLocation, 1, 1, false);
                expLocation = spawnableCopy.getRandomPosition();
            }

            if (expLocation == null) {
                actualExpMexCount = possibleExpMexCount - expMexCountLeft;
                break;
            }

            expansion.fillRect((int) expLocation.x - expSize, (int) expLocation.y - expSize, expSize * 2, expSize * 2, true);
            expansion.intersect(spawnable);

            expMexCount = StrictMath.min((random.nextInt(3) + 2) * 2, expMexCountLeft);
            if (expMexCount >= 6) {
                map.addLargeExpansionMarker(new AIMarker(expLocation, null));
                map.addLargeExpansionMarker(new AIMarker(spawnableCopy.getSymmetryPoint(expLocation), null));
            } else {
                map.addExpansionMarker(new AIMarker(expLocation, null));
                map.addExpansionMarker(new AIMarker(spawnableCopy.getSymmetryPoint(expLocation), null));
            }

            spawnableCopy.fillCircle(expLocation, expSpacing, false);
            spawnableCopy.fillCircle(spawnableCopy.getSymmetryPoint(expLocation), expSpacing, false);

            for (int i = iMex; i < iMex + expMexCount; i += 2) {
                mexLocation = expansion.getRandomPosition();
                if (mexLocation == null) {
                    expMexCount -= i - iMex;
                    break;
                }
                mexSymLocation = expansion.getSymmetryPoint(mexLocation);

                map.getMexes()[i] = new Vector3f(mexLocation);
                map.getMexes()[i + 1] = new Vector3f(mexSymLocation);

                expansion.fillCircle(mexLocation, expMexSpacing, false);
                expansion.fillCircle(mexSymLocation, expMexSpacing, false);

                spawnable.fillCircle(mexLocation, mexSpacing * 2, false);
                spawnable.fillCircle(mexSymLocation, mexSpacing * 2, false);
            }

            iMex += expMexCount;
            expansion.fillCircle(expLocation, expSize + 1, false);
            expMexCountLeft -= expMexCount;
        }
        return actualExpMexCount;
    }

    public void generateHydros(BinaryMask spawnable) {
        int hydroSpacing = 64;
        int iHydro = 0;
        spawnable.fillHalf(false);
        spawnable.fillCenter(64, false);

        for (int i = 0; i < map.getMexes().length; i++) {
            if (map.getMexes()[i] != null) {
                spawnable.fillCircle(map.getMexes()[i].x, map.getMexes()[i].z, 10, false);
            }
        }

        boolean spawnHydro = random.nextBoolean();
        for (int i = 0; i < map.getSpawns().length; i += 2) {
            if (spawnHydro) {
                BinaryMask baseHydro = new BinaryMask(spawnable.getSize(), random.nextLong(), spawnable.getSymmetryHierarchy());
                baseHydro.fillCircle(map.getSpawns()[i + 1], spawnSize * 1.5f, true).fillCircle(map.getSpawns()[i + 1], 20, false).intersect(spawnable);
                for (int j = 0; j < map.getSpawns().length; j += 2) {
                    baseHydro.fillCircle(map.getSpawns()[j], 16, false);
                }
                for (int j = 0; j < iHydro; j += 2) {
                    baseHydro.fillCircle(map.getHydros()[j], 16, false);
                }
                Vector2f location = baseHydro.getRandomPosition();
                if (location == null) {
                    break;
                }
                map.getHydros()[iHydro] = new Vector3f(location);
                Vector2f symLocation = spawnable.getSymmetryPoint(map.getHydros()[iHydro]);
                map.getHydros()[iHydro + 1] = new Vector3f(symLocation);
                spawnable.fillCircle(map.getSpawns()[i + 1], 30, false);
                spawnable.fillCircle(location, hydroSpacing, false);
                iHydro += 2;
            }
        }

        for (int i = 0; i < map.getSpawns().length; i += 2) {
            spawnable.fillCircle(map.getSpawns()[i + 1], spawnSize, false);
        }

        int baseHydroCount = iHydro;

        for (int i = baseHydroCount; i < map.getHydros().length; i += 2) {
            Vector2f hydroLocation = spawnable.getRandomPosition();

            if (hydroLocation == null) {
                break;
            }

            Vector2f hydroSymLocation = spawnable.getSymmetryPoint(hydroLocation);

            map.getHydros()[i] = new Vector3f(hydroLocation);
            map.getHydros()[i + 1] = new Vector3f(hydroSymLocation);

            spawnable.fillCircle(hydroLocation, hydroSpacing, false);
            spawnable.fillCircle(hydroSymLocation, hydroSpacing, false);
        }
    }

    private boolean isMexExpValid(Vector2f location, float size, float density, BinaryMask spawnable) {
        boolean valid = true;
        float count = 0;

        for (int dx = 0; dx < size / 2; dx++) {
            for (int dy = 0; dy < size / 2; dy++) {
                if (spawnable.get(StrictMath.min((int) location.x + dx, map.getSize() - 1), StrictMath.min((int) location.y + dy, map.getSize() - 1))) {
                    count++;
                }
                if (spawnable.get(StrictMath.min((int) location.x + dx, map.getSize() - 1), StrictMath.max((int) location.y - dy, 0))) {
                    count++;
                }
                if (spawnable.get(StrictMath.max((int) location.x - dx, 0), StrictMath.min((int) location.y + dy, map.getSize() - 1))) {
                    count++;
                }
                if (spawnable.get(StrictMath.max((int) location.x - dx, 0), StrictMath.max((int) location.y - dy, 0))) {
                    count++;
                }
            }
        }
        if (count / (size * size) < density) {
            valid = false;
        }
        return valid;
    }

    public void generateAIMarkers(BinaryMask land, BinaryMask unpassable) {
        BinaryMask symmetryRegion = new BinaryMask(land.getSize(), random.nextLong(), land.getSymmetryHierarchy());
        symmetryRegion.fillHalf(true);
        BinaryMask passableLand = land.copy().deflate(map.getSize() / 128f).minus(unpassable.copy().inflate(map.getSize() / 64f)).trimEdge(8).intersect(symmetryRegion);
        BinaryMask water = land.copy().invert().deflate(map.getSize() / 32f).trimEdge(8).intersect(symmetryRegion);
        BinaryMask amphibious = unpassable.copy().invert().deflate(map.getSize() / 64f).trimEdge(8).intersect(symmetryRegion);

        float airMarkerSpacing = map.getSize() / 8f;
        LinkedHashSet<Vector2f> airCoordinates = passableLand.getSpacedCoordinates(airMarkerSpacing);
        Vector2f[] airCoordinatesArray = airCoordinates.toArray(new Vector2f[0]);
        airCoordinates.forEach((location) -> map.addAirMarker(new AIMarker(location, new ArrayList<>())));
        float airMarkerConnectionDistance = (float) StrictMath.sqrt(airMarkerSpacing * airMarkerSpacing * 2) + 1;
        map.getAirAIMarkers().forEach(aiMarker -> aiMarker.getNeighbors().addAll(IntStream.range(0, airCoordinatesArray.length)
                .filter((ind) -> aiMarker.getPosition().getXZDistance(airCoordinatesArray[ind]) < airMarkerConnectionDistance)
                .boxed().collect(Collectors.toList()))
        );

        float amphibiousMarkerSpacing = map.getSize() / 32f;
        LinkedHashSet<Vector2f> amphibiousCoordinates = amphibious.getSpacedCoordinatesEqualTo(true, amphibiousMarkerSpacing);
        LinkedHashSet<Vector2f> symAmphibiousCoordinates = new LinkedHashSet<>();
        amphibiousCoordinates.forEach((location) -> symAmphibiousCoordinates.add(land.getSymmetryPoint(location)));
        amphibiousCoordinates.addAll(symAmphibiousCoordinates);
        Vector2f[] amphibiousCoordinatesArray = amphibiousCoordinates.toArray(new Vector2f[0]);
        amphibiousCoordinates.forEach((location) -> map.addAmphibiousMarker(new AIMarker(location, new ArrayList<>())));
        float amphibiousMarkerConnectionDistance = (float) StrictMath.sqrt(amphibiousMarkerSpacing * amphibiousMarkerSpacing * 2) + 1;
        map.getAmphibiousAIMarkers().forEach(aiMarker -> aiMarker.getNeighbors().addAll(IntStream.range(0, amphibiousCoordinatesArray.length)
                .filter((ind) -> aiMarker.getPosition().getXZDistance(amphibiousCoordinatesArray[ind]) < amphibiousMarkerConnectionDistance)
                .boxed().collect(Collectors.toList()))
        );

        float landMarkerSpacing = map.getSize() / 32f;
        LinkedHashSet<Vector2f> landCoordinates = new LinkedHashSet<>(amphibiousCoordinates);
        landCoordinates.removeIf((location) -> !passableLand.get(location));
        LinkedHashSet<Vector2f> symLandCoordinates = new LinkedHashSet<>();
        landCoordinates.forEach((location) -> symLandCoordinates.add(land.getSymmetryPoint(location)));
        landCoordinates.addAll(symLandCoordinates);
        Vector2f[] landCoordinatesArray = landCoordinates.toArray(new Vector2f[0]);
        landCoordinates.forEach((location) -> map.addLandMarker(new AIMarker(location, new ArrayList<>())));
        float landMarkerConnectionDistance = (float) StrictMath.sqrt(landMarkerSpacing * landMarkerSpacing * 2) + 1;
        map.getLandAIMarkers().forEach(aiMarker -> aiMarker.getNeighbors().addAll(IntStream.range(0, landCoordinatesArray.length)
                .filter((ind) -> aiMarker.getPosition().getXZDistance(landCoordinatesArray[ind]) < landMarkerConnectionDistance)
                .boxed().collect(Collectors.toList()))
        );

        float navyMarkerSpacing = map.getSize() / 16f;
        LinkedHashSet<Vector2f> navyCoordinates = water.getSpacedCoordinatesEqualTo(true, navyMarkerSpacing);
        LinkedHashSet<Vector2f> symNavyCoordinates = new LinkedHashSet<>();
        navyCoordinates.forEach((location) -> symNavyCoordinates.add(land.getSymmetryPoint(location)));
        navyCoordinates.addAll(symNavyCoordinates);
        Vector2f[] navyCoordinatesArray = navyCoordinates.toArray(new Vector2f[0]);
        navyCoordinates.forEach((location) -> map.addNavyMarker(new AIMarker(location, new ArrayList<>())));
        float navyMarkerConnectionDistance = (float) StrictMath.sqrt(navyMarkerSpacing * navyMarkerSpacing * 2) + 1;
        map.getNavyAIMarkers().forEach(aiMarker -> aiMarker.getNeighbors().addAll(IntStream.range(0, navyCoordinatesArray.length)
                .filter((ind) -> aiMarker.getPosition().getXZDistance(navyCoordinatesArray[ind]) < navyMarkerConnectionDistance)
                .boxed().collect(Collectors.toList()))
        );
    }

    public void setMarkerHeights() {
        for (int i = 0; i < map.getSpawns().length; i++) {
            map.getSpawns()[i] = placeOnHeightmap(map, map.getSpawns()[i]);
        }
        for (int i = 0; i < map.getMexes().length; i++) {
            if (map.getMexes()[i] != null) {
                map.getMexes()[i] = placeOnHeightmap(map, map.getMexes()[i]);
            }
        }
        for (int i = 0; i < map.getHydros().length; i++) {
            if (map.getHydros()[i] != null) {
                map.getHydros()[i] = placeOnHeightmap(map, map.getHydros()[i]);
            }
        }
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
