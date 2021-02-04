package neroxis.map;

import lombok.AccessLevel;
import lombok.Getter;
import neroxis.util.Util;

import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

@Getter
public strictfp abstract class ConcurrentMask<T extends Mask<?>> {
    protected final String name;
    protected final Random random;
    protected SymmetrySettings symmetrySettings;
    @Getter(AccessLevel.PROTECTED)
    protected T mask;

    public ConcurrentMask(Long seed, String name) {
        this.name = name;
        if (seed != null) {
            this.random = new Random(seed);
        } else {
            this.random = null;
        }
    }

    abstract public ConcurrentMask<T> mockClone();

    abstract int getSize();

    abstract public String getName();

    abstract public void writeToFile(Path path);

    abstract public String toHash() throws NoSuchAlgorithmException;

    public ConcurrentMask<T> startVisualDebugger() {
        this.mask.startVisualDebugger(name, Util.getStackTraceParentClass());
        return this;
    }

    public ConcurrentMask<T> startVisualDebugger(String maskName) {
        this.mask.startVisualDebugger(maskName, Util.getStackTraceParentClass());
        return this;
    }
}
