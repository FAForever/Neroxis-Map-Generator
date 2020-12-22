package generator;

import map.*;
import util.Vector2f;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

import static util.Placement.placeOnHeightmap;

public strictfp class PropGenerator {
    public static final String[] TREE_GROUPS = {
            "/env/evergreen/props/trees/groups/Brch01_Group01_prop.bp",
            "/env/evergreen/props/trees/groups/Brch01_Group02_prop.bp",
            "/env/evergreen/props/trees/groups/Pine06_GroupA_prop.bp",
            "/env/evergreen/props/trees/groups/Pine06_GroupB_prop.bp",
            "/env/evergreen/props/trees/groups/Pine07_GroupA_prop.bp",
            "/env/evergreen/props/trees/groups/Pine07_GroupB_prop.bp"
    };
    public static final String[] ROCKS = {
            "/env/evergreen/props/rocks/Rock01_prop.bp",
            "/env/evergreen/props/rocks/Rock02_prop.bp",
            "/env/evergreen/props/rocks/Rock03_prop.bp",
            "/env/evergreen/props/rocks/Rock04_prop.bp",
            "/env/evergreen/props/rocks/Rock05_prop.bp"
    };
    public static final String[] BOULDERS = {
            "/env/evergreen/props/rocks/fieldstone01_prop.bp",
            "/env/evergreen/props/rocks/fieldstone02_prop.bp",
            "/env/evergreen/props/rocks/fieldstone03_prop.bp",
            "/env/evergreen/props/rocks/fieldstone04_prop.bp"
    };

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
