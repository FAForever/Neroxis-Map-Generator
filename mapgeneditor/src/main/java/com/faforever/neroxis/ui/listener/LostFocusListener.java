package com.faforever.neroxis.ui.listener;

import lombok.RequiredArgsConstructor;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

@RequiredArgsConstructor
public class LostFocusListener implements FocusListener {
    private final Runnable lostFocusRunnable;

    @Override
    public void focusGained(FocusEvent e) {

    }

    @Override
    public void focusLost(FocusEvent e) {
        lostFocusRunnable.run();
    }
}
