package com.faforever.neroxis.toolsuite.cli;

import com.faforever.neroxis.map.SymmetrySource;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;

public class SourceCompletionCandidates implements Iterable<String> {
    @Override
    public Iterator<String> iterator() {
        Stream<String> symmetrySourceStream = Arrays.stream(SymmetrySource.values()).map(SymmetrySource::name);
        return Stream.concat(Stream.of("Angles 0-360"), symmetrySourceStream).iterator();
    }
}
