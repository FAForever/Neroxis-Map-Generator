package map;

import java.nio.file.Path;

public strictfp interface ConcurrentMask extends Mask {

    ConcurrentMask mockClone();

    String getName();

    void writeToFile(Path path);
}
