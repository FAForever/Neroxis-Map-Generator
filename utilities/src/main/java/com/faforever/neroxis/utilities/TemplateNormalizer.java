package com.faforever.neroxis.utilities;

import com.faforever.neroxis.bases.BaseTemplateLoader;
import com.faforever.neroxis.util.vector.Vector2;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.SequencedMap;
import java.util.SequencedSet;

public class TemplateNormalizer {
    public static void normalizeUnits(SequencedMap<String, SequencedSet<Vector2>> units, Path outputPath) throws
            IOException {
        float maxX = Float.MIN_VALUE;
        float minX = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE;
        for (SequencedSet<Vector2> positions : units.values()) {
            for (Vector2 position : positions) {
                maxX = StrictMath.max(maxX, position.x());
                minX = StrictMath.min(minX, position.x());
                maxY = StrictMath.max(maxY, position.y());
                minY = StrictMath.min(minY, position.y());
            }
        }
        float centerX = (maxX + minX) / 2;
        float centerY = (maxY + minY) / 2;

        Files.createDirectories(outputPath);
        DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(outputPath.toFile())));
        out.writeBytes("Units = {\n");
        int count = 0;
        for (Map.Entry<String, SequencedSet<Vector2>> unitEntry : units.entrySet()) {
            String type = unitEntry.getKey();
            for (Vector2 position : unitEntry.getValue()) {
                out.writeBytes(String.format("\t['UNIT_%d'] = {\n", count));
                out.writeBytes(String.format("\t\ttype = '%s',\n", type));
                out.writeBytes("\t\torders = '',\n");
                out.writeBytes("\t\tplatoon = '',\n");
                out.writeBytes(String.format("\t\tPosition = { %f, 0, %f },\n", position.x() - centerX,
                                             position.y() - centerY));
                out.writeBytes("\t\tOrientation = { 0, 0, 0 },\n");
                out.writeBytes("\t},\n");
                count++;
            }
        }
        out.writeBytes("}\n");
        out.flush();
        out.close();
    }

    void main(String[] args) throws IOException {
        Path templatePath = Paths.get(args[0]);
        Path outputPath = Paths.get(args[1]);
        try (InputStream inputStream = Files.newInputStream(templatePath)) {
            SequencedMap<String, SequencedSet<Vector2>> units;
            if (templatePath.getFileName().toString().contains(".lua")) {
                units = BaseTemplateLoader.loadUnits(inputStream, BaseTemplateLoader.TemplateType.LUA);
            } else if (templatePath.getFileName().toString().contains(".scunits")) {
                units = BaseTemplateLoader.loadUnits(inputStream, BaseTemplateLoader.TemplateType.SCUNITS);
            } else {
                throw new IllegalArgumentException("File format not valid");
            }
            normalizeUnits(units, outputPath);
        }

    }
}
