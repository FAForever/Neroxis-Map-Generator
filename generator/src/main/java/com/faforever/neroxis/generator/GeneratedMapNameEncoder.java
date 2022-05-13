package com.faforever.neroxis.generator;

import java.util.Locale;
import org.apache.commons.codec.CodecPolicy;
import org.apache.commons.codec.binary.Base32;

public class GeneratedMapNameEncoder {
    private static final Base32 ENCODER = new Base32(0, null, false, ((byte) '='), CodecPolicy.LENIENT);

    public static String encode(byte[] bytes) {
        return ENCODER.encodeAsString(bytes).replace("=", "").toLowerCase(Locale.ROOT);
    }

    public static byte[] decode(String encoded) {
        return ENCODER.decode(encoded);
    }
}
