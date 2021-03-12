package neroxis.map;

import lombok.Getter;
import neroxis.util.Pipeline;
import neroxis.util.Vector2f;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;

@Getter
public strictfp class ConcurrentBinaryMask extends ConcurrentMask<BinaryMask> {

    public ConcurrentBinaryMask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        super(seed, name, size);
        this.mask = new BinaryMask(size, seed, symmetrySettings);
        this.symmetrySettings = this.mask.getSymmetrySettings();

        Pipeline.add(this, Collections.emptyList(), res -> this.mask);
    }

    public ConcurrentBinaryMask(ConcurrentBinaryMask mask, Long seed, String name) {
        super(seed, name, mask.getPlannedSize());
        this.mask = new BinaryMask(mask.getPlannedSize(), seed, mask.getSymmetrySettings());

        if (name.equals("mocked")) {
            this.mask = new BinaryMask(mask.getBinaryMask(), seed);
        } else {
            Pipeline.add(this, Collections.singletonList(mask), res ->
                    this.mask.combine(new BinaryMask(((ConcurrentBinaryMask) res.get(0)).getBinaryMask(), seed)));
        }
        this.symmetrySettings = mask.getSymmetrySettings();
    }

    public ConcurrentBinaryMask(BinaryMask mask, Long seed, String name) {
        super(seed, name, mask.getSize());
        this.mask = new BinaryMask(mask, seed);
        this.symmetrySettings = mask.getSymmetrySettings();
    }

    public ConcurrentBinaryMask(ConcurrentFloatMask mask, float threshold, Long seed, String name) {
        super(seed, name, mask.getPlannedSize());
        this.mask = new BinaryMask(mask.getPlannedSize(), seed, mask.getSymmetrySettings());

        if (name.equals("mocked")) {
            this.mask = new BinaryMask(mask.getFloatMask(), threshold, seed);
        } else {
            Pipeline.add(this, Collections.singletonList(mask), res ->
                    this.mask.combine(new BinaryMask(((ConcurrentFloatMask) res.get(0)).getFloatMask(), threshold, seed)));
        }
        this.symmetrySettings = mask.getSymmetrySettings();
    }

    public ConcurrentBinaryMask copy() {
        return new ConcurrentBinaryMask(this, this.mask.getRandom().nextLong(), name + "Copy");
    }

    public ConcurrentBinaryMask clear() {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.clear()
        );
    }

    public ConcurrentBinaryMask init(BinaryMask other) {
        plannedSize = other.getSize();
        return Pipeline.add(this, Arrays.asList(this), res ->
                this.mask.init(other)
        );
    }

    public ConcurrentBinaryMask init(ConcurrentBinaryMask other) {
        plannedSize = other.getPlannedSize();
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.mask.init(((ConcurrentBinaryMask) res.get(1)).getBinaryMask())
        );
    }

    public ConcurrentBinaryMask init(ConcurrentFloatMask other, float threshold) {
        plannedSize = other.getPlannedSize();
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.mask.init(((ConcurrentFloatMask) res.get(1)).getFloatMask(), threshold)
        );
    }

    public ConcurrentBinaryMask randomize(float density) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.randomize(density)
        );
    }

    public ConcurrentBinaryMask flipValues(float density) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.flipValues(density)
        );
    }

    public ConcurrentBinaryMask randomWalk(int numWalkers, int numSteps) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.randomWalk(numWalkers, numSteps)
        );
    }

    public ConcurrentBinaryMask randomWalkWithBrush(Vector2f startingLocation, String brushName, int size, int numberOfUses, float minIntensityForTrue, float maxIntensityForTrue, int maxDistanceBetweenBrushstrokeCenters) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.randomWalkWithBrush(startingLocation, brushName, size, numberOfUses, minIntensityForTrue, maxIntensityForTrue, maxDistanceBetweenBrushstrokeCenters)
        );
    }

    public ConcurrentBinaryMask path(Vector2f start, Vector2f end, float maxStepSize, int numMiddlePoints, float midPointMaxDistance, float midPointMinDistance, float maxAngleError, SymmetryType symmetryType) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.path(start, end, maxStepSize, numMiddlePoints, midPointMaxDistance, midPointMinDistance, maxAngleError, symmetryType)
        );
    }

    public ConcurrentBinaryMask connect(Vector2f start, Vector2f end, float maxStepSize, int numMiddlePoints, float midPointMaxDistance, float midPointMinDistance, float maxAngleError, SymmetryType symmetryType) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.connect(start, end, maxStepSize, numMiddlePoints, midPointMaxDistance, midPointMinDistance, maxAngleError, symmetryType)
        );
    }

    public ConcurrentBinaryMask progressiveWalk(int numWalkers, int numSteps) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.progressiveWalk(numWalkers, numSteps)
        );
    }

    public ConcurrentBinaryMask space(float minSpacing, float maxSpacing) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.space(minSpacing, maxSpacing)
        );
    }

    public ConcurrentBinaryMask invert() {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.invert()
        );
    }

    public ConcurrentBinaryMask enlarge(int size) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.enlarge(size)
        );
    }

    public ConcurrentBinaryMask shrink(int size) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.shrink(size)
        );
    }

    public ConcurrentBinaryMask inflate(float radius) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.inflate(radius)
        );
    }

    public ConcurrentBinaryMask deflate(float radius) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.deflate(radius)
        );
    }

    public ConcurrentBinaryMask cutCorners() {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.cutCorners()
        );
    }

    public ConcurrentBinaryMask grow(float strength, SymmetryType symmetryType, int count) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.grow(strength, symmetryType, count)
        );
    }

    public ConcurrentBinaryMask grow(float strength, SymmetryType symmetryType) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.grow(strength, symmetryType)
        );
    }

    public ConcurrentBinaryMask grow(float strength) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.grow(strength)
        );
    }

    public ConcurrentBinaryMask erode(float strength, SymmetryType symmetryType, int count) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.erode(strength, symmetryType, count)
        );
    }

    public ConcurrentBinaryMask erode(float strength, SymmetryType symmetryType) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.erode(strength, symmetryType)
        );
    }

    public ConcurrentBinaryMask acid(float strength, float size) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.acid(strength, size)
        );
    }

    public ConcurrentBinaryMask erode(float strength) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.erode(strength)
        );
    }

    public ConcurrentBinaryMask outline() {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.outline()
        );
    }

    public ConcurrentBinaryMask smooth(int radius) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.smooth(radius)
        );
    }

    public ConcurrentBinaryMask smooth(int radius, float density) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.smooth(radius, density)
        );
    }

    public ConcurrentBinaryMask replace(ConcurrentBinaryMask other) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.mask.replace(((ConcurrentBinaryMask) res.get(1)).getBinaryMask())
        );
    }

    public ConcurrentBinaryMask combine(ConcurrentBinaryMask other) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.mask.combine(((ConcurrentBinaryMask) res.get(1)).getBinaryMask())
        );
    }

    public ConcurrentBinaryMask intersect(ConcurrentBinaryMask other) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.mask.intersect(((ConcurrentBinaryMask) res.get(1)).getBinaryMask())
        );
    }

    public ConcurrentBinaryMask minus(ConcurrentBinaryMask other) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.mask.minus(((ConcurrentBinaryMask) res.get(1)).getBinaryMask())
        );
    }

    public ConcurrentBinaryMask fillCenter(int extent, boolean value) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.fillCenter(extent, value)
        );
    }

    public ConcurrentBinaryMask fillCircle(float x, float y, float radius, boolean value) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.fillCircle(x, y, radius, value)
        );
    }

    public ConcurrentBinaryMask fillRect(int x, int y, int width, int height, boolean value) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.fillRect(x, y, width, height, value)
        );
    }

    public ConcurrentBinaryMask fillParallelogram(int x, int y, int width, int height, int xSlope, int ySlope, boolean value) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.fillParallelogram(x, y, width, height, xSlope, ySlope, value)
        );
    }

    public ConcurrentBinaryMask fillEdge(int rimWidth, boolean value) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.fillEdge(rimWidth, value)
        );
    }

    public ConcurrentBinaryMask removeAreasSmallerThan(int minArea) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.removeAreasSmallerThan(minArea)
        );
    }

    public ConcurrentBinaryMask fillGaps(int minDistance) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.fillGaps(minDistance)
        );
    }

    public ConcurrentBinaryMask widenGaps(int minDistance) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.widenGaps(minDistance)
        );
    }

    @Override
    public String toHash() throws NoSuchAlgorithmException {
        return mask.toHash();
    }

    protected BinaryMask getBinaryMask() {
        return mask;
    }

    public BinaryMask getFinalMask() {
        Pipeline.await(this);
        return mask.copy();
    }

    @Override
    public ConcurrentBinaryMask mockClone() {
        return new ConcurrentBinaryMask(this, 0L, "mocked");
    }

    public void show() {
        this.mask.show();
    }
}
