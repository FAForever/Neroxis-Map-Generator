package neroxis.generator;

import neroxis.map.*;
import neroxis.util.Vector2f;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

import static neroxis.util.Placement.placeOnHeightmap;

public strictfp class PropGenerator {

    private final SCMap map;
    private final Random random;

    public PropGenerator(SCMap map, long seed) {
        this.map = map;
        random = new Random(seed);
    }

    public void generateProps(BinaryMask spawnable, String[] paths, float separation) {
        generateProps(spawnable, paths, separation, separation);
    }

    public void generateProps(BinaryMask spawnable, String[] paths, float minSeparation, float maxSeparation) {
        spawnable.limitToSymmetryRegion();
        LinkedList<Vector2f> coordinates = spawnable.getRandomCoordinates(minSeparation, maxSeparation);
        coordinates.forEach((location) -> {
            location.add(.5f, .5f);
            Prop prop = new Prop(paths[random.nextInt(paths.length)], location, random.nextFloat() * (float) StrictMath.PI);
            map.addProp(prop);
            ArrayList<SymmetryPoint> symmetryPoints = spawnable.getSymmetryPoints(prop.getPosition(), SymmetryType.SPAWN);
            symmetryPoints.forEach(symmetryPoint -> symmetryPoint.getLocation().roundToNearestHalfPoint());
            ArrayList<Float> symmetryRotation = spawnable.getSymmetryRotation(prop.getRotation());
            for (int i = 0; i < symmetryPoints.size(); i++) {
                Prop symProp = new Prop(prop.getPath(), symmetryPoints.get(i).getLocation(), symmetryRotation.get(i));
                map.addProp(symProp);
            }

        });
    }

    public void setPropHeights() {
        for (Prop prop : map.getProps()) {
            prop.setPosition(placeOnHeightmap(map, prop.getPosition()));
        }
    }
}
