package map;

import generator.VisualDebugger;
import lombok.Getter;
import util.Pipeline;

import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;

@Getter
public strictfp class ConcurrentFloatMask extends ConcurrentMask {

    private FloatMask floatMask;
    private String name;

    public ConcurrentFloatMask(int size, long seed, SymmetryHierarchy symmetryHierarchy, String name) {
        this.floatMask = new FloatMask(size, seed, symmetryHierarchy);
        this.name = name;
        this.symmetryHierarchy = this.floatMask.getSymmetryHierarchy();

        Pipeline.add(this, Collections.emptyList(), Arrays::asList);
    }

    public ConcurrentFloatMask(ConcurrentFloatMask mask, long seed, String name) {
        this.name = name;
        this.floatMask = new FloatMask(mask.getSize(), seed, mask.getSymmetryHierarchy());

        if (name.equals("mocked")) {
            this.floatMask = new FloatMask(mask.getFloatMask(), seed);
        } else {
            Pipeline.add(this, Collections.singletonList(mask), res ->
                    this.floatMask.add(new FloatMask(((ConcurrentFloatMask) res.get(0)).getFloatMask(), this.floatMask.getRandom().nextLong())));
        }
        this.symmetryHierarchy = mask.getSymmetryHierarchy();
    }

    public ConcurrentFloatMask init(ConcurrentBinaryMask other, float low, float high) {
        return Pipeline.add(this, Arrays.asList(this, other), res -> this.floatMask.init(((ConcurrentBinaryMask) res.get(1)).getBinaryMask(), low, high)
        );
    }

    public ConcurrentFloatMask copy(){
        return new ConcurrentFloatMask(this, this.floatMask.getRandom().nextLong(), name+"Copy");
    }

    public ConcurrentFloatMask add(ConcurrentFloatMask other) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.floatMask.add(((ConcurrentFloatMask) res.get(1)).getFloatMask())
        );
    }

    public ConcurrentFloatMask erodeMountains(ConcurrentBinaryMask other) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.floatMask.erodeMountains(((ConcurrentBinaryMask) res.get(1)).getBinaryMask())
        );
    }

    public ConcurrentFloatMask maskToMoutains(ConcurrentBinaryMask other) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.floatMask.maskToMountains(((ConcurrentBinaryMask) res.get(1)).getBinaryMask())
        );
    }

    public ConcurrentFloatMask maskToHeightmap(float underWaterSlope, int maxRepeat, ConcurrentBinaryMask other) {
        return Pipeline.add(this, Arrays.asList(this, other), res ->
                this.floatMask.maskToHeightmap(underWaterSlope, maxRepeat, ((ConcurrentBinaryMask) res.get(1)).getBinaryMask())
        );
    }

    public ConcurrentFloatMask smooth(float radius) {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.floatMask.smooth(radius)
        );
    }

    public ConcurrentFloatMask smooth(float radius, ConcurrentBinaryMask limiter) {
        return Pipeline.add(this, Arrays.asList(this, limiter), res ->
                this.floatMask.smooth(radius, ((ConcurrentBinaryMask) res.get(1)).getBinaryMask())
        );
    }

    public ConcurrentFloatMask gradient() {
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.floatMask.gradient()
        );
    }

    @Override
    public void writeToFile(Path path) {
        floatMask.writeToFile(path);
    }

    @Override
    public String toHash() throws NoSuchAlgorithmException {
        return floatMask.toHash();
    }

    public FloatMask getFloatMask() {
        return floatMask;
    }

    @Override
    public ConcurrentFloatMask mockClone() {
        return new ConcurrentFloatMask(this, 0, "mocked");
    }


    public void startVisualDebugger() {
        VisualDebugger.whitelistMask(this.floatMask);
    }

    @Override
    int getSize() {
        return floatMask.getSize();
    }

    public void startVisualDebugger(String maskName) {
        VisualDebugger.whitelistMask(this.floatMask, maskName);
    }
}
