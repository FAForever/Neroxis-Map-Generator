package map;

import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

public strictfp abstract class ConcurrentMask extends Mask {

    abstract public ConcurrentMask mockClone();

    abstract public String getName();

    abstract public void writeToFile(Path path);

    abstract public String toHash() throws NoSuchAlgorithmException;
}
