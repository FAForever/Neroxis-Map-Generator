package map;

import java.nio.file.Path;

public strictfp abstract class ConcurrentMask extends Mask {

    public abstract ConcurrentMask mockClone();

    public abstract String getName();

    public abstract void writeToFile(Path path);
}
