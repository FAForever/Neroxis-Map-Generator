package generator;

import map.BinaryMask;
import map.Prop;
import map.SCMap;
import util.Vector2f;

import java.util.Random;

import static util.Placement.placeOnHeightmap;

public strictfp class PropGenerator {
    public final String[] TREE_GROUPS = {
            "/env/evergreen/props/trees/groups/Brch01_Group01_prop.bp",
            "/env/evergreen/props/trees/groups/Brch01_Group02_prop.bp",
            "/env/evergreen/props/trees/groups/Pine06_GroupA_prop.bp",
            "/env/evergreen/props/trees/groups/Pine06_GroupB_prop.bp",
            "/env/evergreen/props/trees/groups/Pine07_GroupA_prop.bp",
            "/env/evergreen/props/trees/groups/Pine07_GroupB_prop.bp"
    };
    public final String[] ROCKS = {
            "/env/evergreen/props/rocks/Rock01_prop.bp",
            "/env/evergreen/props/rocks/Rock02_prop.bp",
            "/env/evergreen/props/rocks/Rock03_prop.bp",
            "/env/evergreen/props/rocks/Rock04_prop.bp",
            "/env/evergreen/props/rocks/Rock05_prop.bp"
    };
    public final String[] FIELD_STONES = {
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
        BinaryMask spawnableCopy = new BinaryMask(spawnable, random.nextLong());
        spawnableCopy.fillHalf(false);
        Vector2f location = spawnableCopy.getRandomPosition();
        Vector2f symLocation;
        while (location != null) {
            symLocation = spawnableCopy.getSymmetryPoint(location);
            spawnableCopy.fillCircle(location, separation, false);
            Prop prop1 = new Prop(paths[random.nextInt(paths.length)], location, random.nextFloat() * (float) StrictMath.PI);
            Prop prop2 = new Prop(prop1.getPath(), symLocation, spawnableCopy.getReflectedRotation(prop1.getRotation()));
            map.addProp(prop1);
            map.addProp(prop2);
            location = spawnableCopy.getRandomPosition();
        }
    }

    public void setPropHeights() {
        for (Prop prop : map.getProps()) {
            prop.setPosition(placeOnHeightmap(map, prop.getPosition()));
        }
    }
}
