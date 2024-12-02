package com.faforever.neroxis.bases;

import com.faforever.neroxis.util.vector.Vector2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Map;
import java.util.SequencedMap;
import java.util.SequencedSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.CONCURRENT)
public class BaseTemplateLoaderTest {

    @Test
    public void testLoadUnitsFromLua() throws Exception {
        SequencedMap<String, SequencedSet<Vector2>> units = BaseTemplateLoader.loadUnits("/base_template/Test.lua");
        assertEquals(2, units.size());
        Map.Entry<String, SequencedSet<Vector2>> firstEntry = units.firstEntry();
        assertEquals("uab1301", firstEntry.getKey());
        SequencedSet<Vector2> firstEntryPositions = firstEntry.getValue();
        assertEquals(2, firstEntryPositions.size());
        assertEquals(new Vector2(0f, .5f), firstEntryPositions.getFirst());
        assertEquals(new Vector2(6f, 3.5f), firstEntryPositions.getLast());

        Map.Entry<String, SequencedSet<Vector2>> secondEntry = units.lastEntry();
        assertEquals("uab5101", secondEntry.getKey());
        SequencedSet<Vector2> secondEntryPositions = secondEntry.getValue();
        assertEquals(2, secondEntryPositions.size());
        assertEquals(new Vector2(-11f, .5f), secondEntryPositions.getFirst());
        assertEquals(new Vector2(-10f, .5f), secondEntryPositions.getLast());
    }

    @Test
    public void testLoadUnitsFromSCUnits() throws Exception {
        SequencedMap<String, SequencedSet<Vector2>> units = BaseTemplateLoader.loadUnits("/base_template/Test.scunits");
        Map.Entry<String, SequencedSet<Vector2>> firstEntry = units.firstEntry();
        assertEquals("uab1301", firstEntry.getKey());
        SequencedSet<Vector2> firstEntryPositions = firstEntry.getValue();
        assertEquals(2, firstEntryPositions.size());
        assertEquals(new Vector2(0f, .5f), firstEntryPositions.getFirst());
        assertEquals(new Vector2(6f, 3.5f), firstEntryPositions.getLast());

        Map.Entry<String, SequencedSet<Vector2>> secondEntry = units.lastEntry();
        assertEquals("uab5101", secondEntry.getKey());
        SequencedSet<Vector2> secondEntryPositions = secondEntry.getValue();
        assertEquals(2, secondEntryPositions.size());
        assertEquals(new Vector2(-11f, .5f), secondEntryPositions.getFirst());
        assertEquals(new Vector2(-10f, .5f), secondEntryPositions.getLast());
    }

}
