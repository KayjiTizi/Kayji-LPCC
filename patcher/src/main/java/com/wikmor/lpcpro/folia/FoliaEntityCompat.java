package com.wikmor.lpcpro.folia;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public final class FoliaEntityCompat {
    private FoliaEntityCompat() {
    }

    public static boolean teleport(Entity entity, Location location) {
        if (!FoliaScheduler.isFolia()) {
            return entity.teleport(location);
        }
        try {
            Method method = entity.getClass().getMethod("teleportAsync", Location.class);
            method.setAccessible(true);
            Object result = method.invoke(entity, location);
            if (result instanceof CompletableFuture<?> future) {
                return !future.isCompletedExceptionally();
            }
            return result == null || Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException exception) {
            Plugin plugin = JavaPlugin.getProvidingPlugin(FoliaEntityCompat.class);
            FoliaScheduler.runEntity(plugin, entity, () -> entity.teleport(location));
            return true;
        }
    }

    public static void remove(Entity entity) {
        if (!FoliaScheduler.isFolia()) {
            entity.remove();
            return;
        }
        Plugin plugin = JavaPlugin.getProvidingPlugin(FoliaEntityCompat.class);
        FoliaScheduler.runEntity(plugin, entity, entity::remove);
    }

    public static boolean isDead(Entity entity) {
        if (FoliaScheduler.isFolia()) {
            return entity == null;
        }
        try {
            return entity == null || entity.isDead();
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
