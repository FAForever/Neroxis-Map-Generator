package map;

import generator.VisualDebugger;
import util.Pipeline;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

public strictfp class ConcurrentBinaryMask implements ConcurrentMask {

    private BinaryMask binaryMask;
    private String name;

    public ConcurrentBinaryMask(int size, long seed, SymmetryHierarchy symmetryHierarchy, String name) {
        this.binaryMask = new BinaryMask(size, seed, symmetryHierarchy);
        this.name = name;

        Pipeline.add(this, Collections.emptyList(), Arrays::asList);
    }

    public ConcurrentBinaryMask(int size, long seed, Symmetry symmetry, String name) {
        this.binaryMask = new BinaryMask(size, seed, symmetry);
        this.name = name;

        Pipeline.add(this, Collections.emptyList(), Arrays::asList);
    }

    public ConcurrentBinaryMask(ConcurrentBinaryMask mask, long seed, String name) {
        this.name = name;

        if (name.equals("mocked")) {
            this.binaryMask = new BinaryMask(mask.getBinaryMask(), seed);
        } else {
            Pipeline.add(this, Collections.singletonList(mask), res -> {
                this.binaryMask = new BinaryMask(((ConcurrentBinaryMask) res.get(0)).getBinaryMask(), seed);
                return Collections.singletonList(this.binaryMask);
            });
        }
    }

    public ConcurrentBinaryMask(BinaryMask mask, long seed, String name) {
        this.name = name;
        this.binaryMask = new BinaryMask(mask, seed);
    }

    public ConcurrentBinaryMask randomize(float density) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.randomize(density)
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

    public ConcurrentBinaryMask acid(float strength, Symmetry symmetry) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.acid(strength, symmetry)
        );
    }

    public ConcurrentBinaryMask acid(float strength) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.binaryMask.acid(strength)
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


    @Override
    public void writeToFile(Path path) {
        binaryMask.writeToFile(path);
    }

    public BinaryMask getBinaryMask() {
        return binaryMask;
    }

    @Override
    public ConcurrentBinaryMask mockClone() {
        return new ConcurrentBinaryMask(this, 0, "mocked");
    }

    @Override
    public String getName() {
        return name;
    }

    public void startVisualDebugger() {
        VisualDebugger.whitelistMask(this.binaryMask);
    }

    public void startVisualDebugger(String maskName) {
        VisualDebugger.whitelistMask(this.binaryMask, maskName);
    }
}
