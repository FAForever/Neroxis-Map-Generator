package com.faforever.neroxis.util;

import com.faforever.neroxis.map.mask.Mask;

public strictfp class VisualDebugger {

    public static boolean VISUALIZE_ALL = false;

    public synchronized static void createGUI() {
        if (!VisualDebuggerGui.isCreated()) {
            VisualDebuggerGui.createGui();
        }
    }

    public static void visualizeMask(Mask<?, ?> mask, String method) {
        visualizeMask(mask, method, null);
    }

    public static void visualizeMask(Mask<?, ?> mask, String method, String line) {
        if ((mask.isVisualDebug() && Util.DEBUG) || Util.VISUALIZE || VISUALIZE_ALL) {
            VisualDebugger.createGUI();
            String name = mask.getVisualName();
            name = name == null ? mask.getName() : name;
            VisualDebuggerGui.update(name + " " + method + " " + line, mask.mock());
        }
    }
}
