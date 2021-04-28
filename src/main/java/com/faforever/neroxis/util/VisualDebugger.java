package com.faforever.neroxis.util;

import com.faforever.neroxis.map.mask.Mask;

public strictfp class VisualDebugger {

    public static boolean ENABLED = Util.DEBUG;

    public synchronized static void createGUI() {
        if (!VisualDebuggerGui.isCreated()) {
            VisualDebuggerGui.createGui();
        }
    }

    public static void visualizeMask(Mask<?, ?> mask, String method) {
        visualizeMask(mask, method, null);
    }

    public static void visualizeMask(Mask<?, ?> mask, String method, String line) {
        if (!mask.isVisualDebug()) {
            return;
        }
        VisualDebuggerGui.update(mask.getVisualName() + " " + method + " " + line, mask.copy());
    }
}
