package com.wikmor.lpcpro.bubble;

import com.wikmor.lpcpro.folia.FoliaDisplayCompat;
import com.wikmor.lpcpro.folia.FoliaEntityCompat;
import com.wikmor.lpcpro.folia.FoliaScheduler;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

public final class FoliaChatBubbleCompat {
    private static final Field ACTIVE_BUBBLES = field("activeBubbles");
    private static final Field FOLLOW_TASKS = field("followTasks");

    private FoliaChatBubbleCompat() {
    }

    public static void removeBubbles(ChatBubbleManager manager, Player player) {
        UUID playerId = player.getUniqueId();
        BukkitTask followTask = followTasks(manager).remove(playerId);
        if (followTask != null) {
            try {
                followTask.cancel();
            } catch (RuntimeException ignored) {
            }
        }

        Object bubble = activeBubbles(manager).remove(playerId);
        if (bubble == null) {
            return;
        }

        BukkitTask removalTask = removalTask(bubble);
        if (removalTask != null) {
            try {
                removalTask.cancel();
            } catch (RuntimeException ignored) {
            }
        }

        Entity entity = entity(bubble);
        if (entity != null) {
            FoliaEntityCompat.remove(entity);
        }
    }

    public static void followTick(BukkitRunnable task, ChatBubbleManager manager, Player player,
                                  Object bubble, float heightOffset, float scale) {
        UUID playerId = player.getUniqueId();
        if (!player.isOnline() || !activeBubbles(manager).containsKey(playerId)) {
            cancelFollow(task, manager, playerId);
            return;
        }
        Entity entity = entity(bubble);
        if (!(entity instanceof TextDisplay textDisplay)) {
            cancelFollow(task, manager, playerId);
            return;
        }
        Location location;
        try {
            location = player.getLocation();
        } catch (RuntimeException exception) {
            removeBubble(task, manager, playerId, entity);
            return;
        }
        if (location == null) {
            removeBubble(task, manager, playerId, entity);
            return;
        }
        if (!FoliaScheduler.isFolia()) {
            updateDisplay(task, manager, playerId, textDisplay, location, heightOffset, scale);
            return;
        }
        Plugin plugin = JavaPlugin.getProvidingPlugin(FoliaChatBubbleCompat.class);
        FoliaScheduler.runEntity(plugin, entity,
                () -> updateDisplay(task, manager, playerId, textDisplay, location, heightOffset, scale));
    }

    private static void updateDisplay(BukkitRunnable task, ChatBubbleManager manager, UUID playerId,
                                      TextDisplay textDisplay, Location location, float heightOffset, float scale) {
        try {
            if (textDisplay.isDead()) {
                cancelFollow(task, manager, playerId);
                return;
            }
            if (!textDisplay.getWorld().equals(location.getWorld())) {
                removeBubble(task, manager, playerId, textDisplay);
                return;
            }
            FoliaEntityCompat.teleport(textDisplay, location.clone());
            FoliaDisplayCompat.setInterpolationDuration(textDisplay, 2);
            FoliaDisplayCompat.setInterpolationDelay(textDisplay, 0);
            FoliaDisplayCompat.setTransformation(textDisplay, new Transformation(
                    new Vector3f(0.0f, heightOffset, 0.0f),
                    new Quaternionf(),
                    new Vector3f(scale, scale, scale),
                    new Quaternionf()));
        } catch (RuntimeException exception) {
            removeBubble(task, manager, playerId, textDisplay);
        }
    }

    private static void removeBubble(BukkitRunnable task, ChatBubbleManager manager, UUID playerId, Entity entity) {
        cancelFollow(task, manager, playerId);
        activeBubbles(manager).remove(playerId);
        if (entity != null) {
            FoliaEntityCompat.remove(entity);
        }
    }

    private static void cancelFollow(BukkitRunnable task, ChatBubbleManager manager, UUID playerId) {
        try {
            task.cancel();
        } catch (RuntimeException ignored) {
        }
        BukkitTask previous = followTasks(manager).remove(playerId);
        if (previous != null) {
            previous.cancel();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, Object> activeBubbles(ChatBubbleManager manager) {
        try {
            return (Map<UUID, Object>) ACTIVE_BUBBLES.get(manager);
        } catch (IllegalAccessException exception) {
            throw new RuntimeException("Cannot access active chat bubbles", exception);
        }
    }

    private static Entity entity(Object bubble) {
        if (bubble == null) {
            return null;
        }
        try {
            Field field = bubble.getClass().getDeclaredField("entity");
            field.setAccessible(true);
            return (Entity) field.get(bubble);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static BukkitTask removalTask(Object bubble) {
        if (bubble == null) {
            return null;
        }
        try {
            Field field = bubble.getClass().getDeclaredField("removalTask");
            field.setAccessible(true);
            return (BukkitTask) field.get(bubble);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, BukkitTask> followTasks(ChatBubbleManager manager) {
        try {
            return (Map<UUID, BukkitTask>) FOLLOW_TASKS.get(manager);
        } catch (IllegalAccessException exception) {
            throw new RuntimeException("Cannot access chat bubble follow tasks", exception);
        }
    }

    private static Field field(String name) {
        try {
            Field field = ChatBubbleManager.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
