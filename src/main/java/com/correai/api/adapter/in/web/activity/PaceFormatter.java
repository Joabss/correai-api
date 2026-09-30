package com.correai.api.adapter.in.web.activity;

/**
 * Presentation-level helper to format a pace (seconds/km) as mm:ss.
 */
public final class PaceFormatter {

    private PaceFormatter() {
    }

    public static String format(Integer paceSeconds) {
        if (paceSeconds == null) {
            return null;
        }
        int min = paceSeconds / 60;
        int sec = paceSeconds % 60;
        return String.format("%02d:%02d", min, sec);
    }
}

