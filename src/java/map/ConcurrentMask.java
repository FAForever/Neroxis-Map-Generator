package map;

import lombok.Getter;

import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

@Getter
public strictfp abstract class ConcurrentMask<T extends Mask<?>> {
    protected final String name;
    protected final Random random;
    protected T mask;
    protected SymmetrySettings symmetrySettings;

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
}
