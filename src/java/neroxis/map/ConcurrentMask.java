package neroxis.map;

import lombok.AccessLevel;
import lombok.Getter;
import neroxis.util.Pipeline;
import neroxis.util.Util;

import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Random;

@Getter
public strictfp abstract class ConcurrentMask<T extends Mask<?>> {
    protected final String name;
    protected final Random random;
    protected SymmetrySettings symmetrySettings;
    protected int plannedSize;
    @Getter(AccessLevel.PROTECTED)
    protected T mask;

    public ConcurrentMask(Long seed, String name, int plannedSize) {
        this.name = name;
        this.plannedSize = plannedSize;
        if (seed != null) {
            this.random = new Random(seed);
        } else {
            this.random = null;
        }
    }

    abstract public ConcurrentMask<T> mockClone();

    abstract public String toHash() throws NoSuchAlgorithmException;

    public ConcurrentMask<?> setSize(int size) {
        plannedSize = size;
        return Pipeline.add(this, Collections.singletonList(this), res ->
                this.mask.setSize(size)
        );
    }

    public ConcurrentMask<T> startVisualDebugger() {
        this.mask.startVisualDebugger(name, Util.getStackTraceParentClass());
        return this;
    }

    public ConcurrentMask<T> startVisualDebugger(String maskName) {
        this.mask.startVisualDebugger(maskName, Util.getStackTraceParentClass());
        return this;
    }
}
