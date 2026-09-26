package com.wikmor.lpcpro.folia;

import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;

public final class FoliaDisplayCompat {
    private FoliaDisplayCompat() {
    }

    public static void setInterpolationDuration(Display display, int duration) {
        run(display, () -> display.setInterpolationDuration(duration));
    }

    public static void setInterpolationDelay(Display display, int delay) {
        run(display, () -> display.setInterpolationDelay(delay));
    }

    public static void setTransformation(Display display, Transformation transformation) {
        run(display, () -> display.setTransformation(transformation));
    }

    public static void setBillboard(Display display, Display.Billboard billboard) {
        run(display, () -> display.setBillboard(billboard));
    }

    public static void setText(TextDisplay display, String text) {
        run(display, () -> display.setText(text));
    }

    public static void setShadowed(TextDisplay display, boolean shadowed) {
        run(display, () -> display.setShadowed(shadowed));
    }

    public static void setAlignment(TextDisplay display, TextDisplay.TextAlignment alignment) {
        run(display, () -> display.setAlignment(alignment));
    }

    public static void setLineWidth(TextDisplay display, int width) {
        run(display, () -> display.setLineWidth(width));
    }

    public static void setDefaultBackground(TextDisplay display, boolean defaultBackground) {
        run(display, () -> display.setDefaultBackground(defaultBackground));
    }

    public static void setBackgroundColor(TextDisplay display, Color color) {
        run(display, () -> display.setBackgroundColor(color));
    }

    public static void setPersistent(Entity entity, boolean persistent) {
        run(entity, () -> entity.setPersistent(persistent));
    }

    private static void run(Entity entity, Runnable runnable) {
        if (!FoliaScheduler.isFolia()) {
            runnable.run();
            return;
        }
        FoliaScheduler.runEntity(org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(FoliaDisplayCompat.class), entity, runnable);
    }
}
