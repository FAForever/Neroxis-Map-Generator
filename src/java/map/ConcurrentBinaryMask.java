package map;

import generator.VisualDebugger;
import lombok.Getter;
import util.Pipeline;

import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;

@Getter
public strictfp class ConcurrentBinaryMask extends ConcurrentMask {

    private BinaryMask binaryMask;
    private final String name;

    public ConcurrentBinaryMask(int size, long seed, SymmetryHierarchy symmetryHierarchy, String name) {
        this.binaryMask = new BinaryMask(size, seed, symmetryHierarchy);
        this.name = name;
        this.symmetryHierarchy = this.binaryMask.getSymmetryHierarchy();

        Pipeline.add(this, Collections.emptyList(), Arrays::asList);
    }

    public ConcurrentBinaryMask(int size, long seed, Symmetry symmetry, String name) {
        this.binaryMask = new BinaryMask(size, seed, symmetry);
        this.name = name;
        this.symmetryHierarchy = this.binaryMask.getSymmetryHierarchy();

        Pipeline.add(this, Collections.emptyList(), Arrays::asList);
    }

    public ConcurrentBinaryMask(ConcurrentBinaryMask mask, long seed, String name) {
        this.name = name;
        this.binaryMask = new BinaryMask(1, seed, mask.getSymmetryHierarchy());

        if (name.equals("mocked")) {
            this.binaryMask = new BinaryMask(mask.getBinaryMask(), seed);
        } else {
            Pipeline.add(this, Collections.singletonList(mask), res ->
                this.binaryMask.combine(new BinaryMask(((ConcurrentBinaryMask) res.get(0)).getBinaryMask(), seed)));
        }
        this.symmetryHierarchy = mask.getSymmetryHierarchy();
    }

    public ConcurrentBinaryMask(BinaryMask mask, long seed, String name) {
        this.name = name;
        this.binaryMask = new BinaryMask(mask, seed);
        this.symmetryHierarchy = mask.getSymmetryHierarchy();
    }

    public ConcurrentBinaryMask(FloatMask mask, float threshold, long seed, String name) {
        this.name = name;
        this.binaryMask = new BinaryMask(mask, threshold, seed);
        this.symmetryHierarchy = mask.getSymmetryHierarchy();
    }

    public ConcurrentBinaryMask(ConcurrentFloatMask mask, float threshold, long seed, String name) {
        this.name = name;
        this.binaryMask = new BinaryMask(1, seed, mask.getSymmetryHierarchy());

        if (name.equals("mocked")) {
            this.binaryMask = new BinaryMask(mask.getFloatMask(), threshold, seed);
        } else {
            Pipeline.add(this, Collections.singletonList(mask), res ->
                    this.binaryMask.combine(new BinaryMask(((ConcurrentFloatMask) res.get(0)).getFloatMask(), threshold, seed)));
        }
        this.symmetryHierarchy = mask.getSymmetryHierarchy();
    }

    public ConcurrentBinaryMask copy(){
        return new ConcurrentBinaryMask(this, this.binaryMask.getRandom().nextLong(), name+"Copy");
    }

    public ConcurrentBinaryMask randomize(float density) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.randomize(density)
        );
    }

    public ConcurrentBinaryMask flipValues(float density) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.flipValues(density)
        );
    }

    public ConcurrentBinaryMask flipValues(float density, Symmetry symmetry) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.flipValues(density, symmetry)
        );
    }

    public ConcurrentBinaryMask randomWalk(int numWalkers, int numSteps) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.randomWalk(numWalkers, numSteps)
        );
    }

    public ConcurrentBinaryMask invert() {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.invert()
        );
    }

    public ConcurrentBinaryMask enlarge(int size) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.enlarge(size)
        );
    }

    public ConcurrentBinaryMask shrink(int size) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.shrink(size)
        );
    }

    public ConcurrentBinaryMask inflate(float radius) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.inflate(radius)
        );
    }

    public ConcurrentBinaryMask deflate(float radius) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.deflate(radius)
        );
    }

    public ConcurrentBinaryMask cutCorners() {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.cutCorners()
        );
    }

    public ConcurrentBinaryMask grow(float strength, Symmetry symmetry, int count) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.grow(strength, symmetry, count)
        );
    }

    public ConcurrentBinaryMask grow(float strength, Symmetry symmetry) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.grow(strength, symmetry)
        );
    }

    public ConcurrentBinaryMask erode(float strength, Symmetry symmetry, int count) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.erode(strength, symmetry, count)
        );
    }

    public ConcurrentBinaryMask erode(float strength, Symmetry symmetry) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.erode(strength, symmetry)
        );
    }

    public ConcurrentBinaryMask acid(float strength, float size) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.acid(strength, size)
        );
    }

    public ConcurrentBinaryMask erode(float strength) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.erode(strength)
        );
    }

    public ConcurrentBinaryMask outline() {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.outline()
        );
    }

    public ConcurrentBinaryMask smooth(float radius) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.smooth(radius)
        );
    }

    public ConcurrentBinaryMask smooth(float radius, float density) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.smooth(radius, density)
        );
    }

    public ConcurrentBinaryMask combine(ConcurrentBinaryMask other) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.binaryMask.combine(((ConcurrentBinaryMask) res.get(1)).getBinaryMask())
        );
    }

    public ConcurrentBinaryMask intersect(ConcurrentBinaryMask other) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.binaryMask.intersect(((ConcurrentBinaryMask) res.get(1)).getBinaryMask())
        );
    }

    public ConcurrentBinaryMask minus(ConcurrentBinaryMask other) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.binaryMask.minus(((ConcurrentBinaryMask) res.get(1)).getBinaryMask())
        );
    }

    public ConcurrentBinaryMask fillCenter(int extent, boolean value) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.fillCenter(extent, value)
        );
    }

    public ConcurrentBinaryMask fillCircle(float x, float y, float radius, boolean value) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.fillCircle(x, y, radius, value)
        );
    }

    public ConcurrentBinaryMask fillRect(int x, int y, int width, int height, boolean value) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.fillRect(x, y, width, height, value)
        );
    }

    public ConcurrentBinaryMask fillParallelogram(int x, int y, int width, int height, int xSlope, int ySlope, boolean value) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.fillParallelogram(x, y, width, height, xSlope, ySlope, value)
        );
    }

    public ConcurrentBinaryMask trimEdge(int rimWidth) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.trimEdge(rimWidth)
        );
    }

    public ConcurrentBinaryMask filterShapes(int minArea) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.filterShapes(minArea)
        );
    }


    @Override
    public void writeToFile(Path path) {
        binaryMask.writeToFile(path);
    }

    @Override
    public String toHash() throws NoSuchAlgorithmException {
        return binaryMask.toHash();
    }

    public BinaryMask getFinalMask() {
        Pipeline.await(this);
        return binaryMask;
    }

    @Override
    public ConcurrentBinaryMask mockClone() {
        return new ConcurrentBinaryMask(this, 0, "mocked");
    }

    public void startVisualDebugger() {
        VisualDebugger.whitelistMask(this.binaryMask);
    }

    @Override
    int getSize() {
        return binaryMask.getSize();
    }

    public void startVisualDebugger(String maskName) {
        VisualDebugger.whitelistMask(this.binaryMask, maskName);
    }
}
