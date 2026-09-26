package com.wikmor.lpcpro.folia;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class FoliaScheduler {
    private static final boolean FOLIA = hasMethod(Bukkit.class, "getGlobalRegionScheduler");
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);
    private static final Map<Plugin, Map<Integer, Object>> TASKS = new ConcurrentHashMap<>();

    private FoliaScheduler() {
    }

    public static BukkitTask runTask(Plugin plugin, Runnable runnable) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(runnable, "runnable");
        if (!FOLIA) {
            return Bukkit.getScheduler().runTask(plugin, runnable);
        }
        Entity entity = findCapturedEntity(runnable);
        if (entity != null) {
            return scheduleEntity(plugin, entity, task -> runnable.run(), 0L, 0L, false);
        }
        return scheduleGlobal(plugin, task -> runnable.run(), 0L, 0L, false);
    }

    public static BukkitTask runEntity(Plugin plugin, Entity entity, Runnable runnable) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(runnable, "runnable");
        if (!FOLIA) {
            return Bukkit.getScheduler().runTask(plugin, runnable);
        }
        return scheduleEntity(plugin, entity, task -> runnable.run(), 0L, 0L, false);
    }

    public static BukkitTask runGlobal(Plugin plugin, Runnable runnable) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(runnable, "runnable");
        if (!FOLIA) {
            return Bukkit.getScheduler().runTask(plugin, runnable);
        }
        return scheduleGlobal(plugin, task -> runnable.run(), 0L, 0L, false);
    }

    public static BukkitTask runRegion(Plugin plugin, Location location, Runnable runnable) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(runnable, "runnable");
        if (!FOLIA) {
            return Bukkit.getScheduler().runTask(plugin, runnable);
        }
        return scheduleRegion(plugin, location, task -> runnable.run(), 0L, 0L, false);
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static BukkitTask runTaskLater(Plugin plugin, Runnable runnable, long delay) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(runnable, "runnable");
        if (!FOLIA) {
            return Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
        }
        Entity entity = findCapturedEntity(runnable);
        if (entity != null) {
            return scheduleEntity(plugin, entity, task -> runnable.run(), delay, 0L, false);
        }
        return scheduleGlobal(plugin, task -> runnable.run(), delay, 0L, false);
    }

    public static BukkitTask runTaskTimer(Plugin plugin, Runnable runnable, long delay, long period) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(runnable, "runnable");
        if (!FOLIA) {
            return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period);
        }
        Entity entity = findCapturedEntity(runnable);
        if (entity != null) {
            return scheduleEntity(plugin, entity, task -> runnable.run(), delay, Math.max(1L, period), true);
        }
        return scheduleGlobal(plugin, task -> runnable.run(), delay, Math.max(1L, period), true);
    }

    public static BukkitTask runTaskAsynchronously(Plugin plugin, Runnable runnable) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(runnable, "runnable");
        if (!FOLIA) {
            return Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
        return scheduleAsync(plugin, task -> runnable.run(), 0L, 0L, false);
    }

    public static BukkitTask runTaskLaterAsynchronously(Plugin plugin, Runnable runnable, long delay) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(runnable, "runnable");
        if (!FOLIA) {
            return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
        }
        return scheduleAsync(plugin, task -> runnable.run(), delay, 0L, false);
    }

    public static BukkitTask runTaskTimerAsynchronously(Plugin plugin, Runnable runnable, long delay, long period) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(runnable, "runnable");
        if (!FOLIA) {
            return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period);
        }
        return scheduleAsync(plugin, task -> runnable.run(), delay, Math.max(1L, period), true);
    }

    public static BukkitTask runBukkitRunnableTimer(BukkitRunnable runnable, Plugin plugin, long delay, long period) {
        Objects.requireNonNull(runnable, "runnable");
        BukkitTask task;
        Entity entity = findCapturedEntity(runnable);
        if (FOLIA && entity != null) {
            task = scheduleEntity(plugin, entity, scheduledTask -> runnable.run(), delay, Math.max(1L, period), true);
        } else {
            task = runTaskTimer(plugin, runnable::run, delay, period);
        }
        attachBukkitRunnableTask(runnable, task);
        return task;
    }

    public static BukkitTask runBukkitRunnableAsynchronously(BukkitRunnable runnable, Plugin plugin) {
        Objects.requireNonNull(runnable, "runnable");
        BukkitTask task = runTaskAsynchronously(plugin, runnable::run);
        attachBukkitRunnableTask(runnable, task);
        return task;
    }

    public static BukkitTask runBukkitRunnableLaterAsynchronously(BukkitRunnable runnable, Plugin plugin, long delay) {
        Objects.requireNonNull(runnable, "runnable");
        BukkitTask task = runTaskLaterAsynchronously(plugin, runnable::run, delay);
        attachBukkitRunnableTask(runnable, task);
        return task;
    }

    public static BukkitTask runBukkitRunnableTimerAsynchronously(BukkitRunnable runnable, Plugin plugin, long delay, long period) {
        Objects.requireNonNull(runnable, "runnable");
        BukkitTask task = runTaskTimerAsynchronously(plugin, runnable::run, delay, period);
        attachBukkitRunnableTask(runnable, task);
        return task;
    }

    public static BukkitTask runBukkitRunnable(BukkitRunnable runnable, Plugin plugin) {
        Objects.requireNonNull(runnable, "runnable");
        BukkitTask task;
        Entity entity = findCapturedEntity(runnable);
        if (FOLIA && entity != null) {
            task = scheduleEntity(plugin, entity, scheduledTask -> runnable.run(), 0L, 0L, false);
        } else {
            task = runTask(plugin, runnable::run);
        }
        attachBukkitRunnableTask(runnable, task);
        return task;
    }

    public static BukkitTask runBukkitRunnableLater(BukkitRunnable runnable, Plugin plugin, long delay) {
        Objects.requireNonNull(runnable, "runnable");
        BukkitTask task;
        Entity entity = findCapturedEntity(runnable);
        if (FOLIA && entity != null) {
            task = scheduleEntity(plugin, entity, scheduledTask -> runnable.run(), delay, 0L, false);
        } else {
            task = runTaskLater(plugin, runnable::run, delay);
        }
        attachBukkitRunnableTask(runnable, task);
        return task;
    }

    public static void cancelTasks(Plugin plugin) {
        if (!FOLIA) {
            Bukkit.getScheduler().cancelTasks(plugin);
            return;
        }
        Map<Integer, Object> tasks = TASKS.remove(plugin);
        if (tasks != null) {
            for (Object task : tasks.values()) {
                cancelScheduledTask(task);
            }
        }
    }

    private static BukkitTask scheduleGlobal(Plugin plugin, Consumer<Object> consumer, long delay, long period, boolean repeating) {
        try {
            Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
            Object task;
            Consumer<Object> wrapped = scheduledTask -> consumer.accept(scheduledTask);
            if (repeating) {
                Method method = scheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
                method.setAccessible(true);
                task = method
                        .invoke(scheduler, plugin, wrapped, normalizeDelay(delay), period);
            } else if (delay > 0L) {
                Method method = scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
                method.setAccessible(true);
                task = method
                        .invoke(scheduler, plugin, wrapped, delay);
            } else {
                Method method = scheduler.getClass().getMethod("run", Plugin.class, Consumer.class);
                method.setAccessible(true);
                task = method
                        .invoke(scheduler, plugin, wrapped);
            }
            return wrap(plugin, task, false);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Failed to schedule Folia global task", exception);
        }
    }

    private static BukkitTask scheduleEntity(Plugin plugin, Entity entity, Consumer<Object> consumer, long delay, long period, boolean repeating) {
        try {
            Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
            Object task;
            Consumer<Object> wrapped = scheduledTask -> consumer.accept(scheduledTask);
            Runnable retired = () -> {
            };
            if (repeating) {
                Method method = scheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, Runnable.class, long.class, long.class);
                method.setAccessible(true);
                task = method
                        .invoke(scheduler, plugin, wrapped, retired, normalizeDelay(delay), period);
            } else if (delay > 0L) {
                Method method = scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class);
                method.setAccessible(true);
                task = method
                        .invoke(scheduler, plugin, wrapped, retired, delay);
            } else {
                Method method = scheduler.getClass().getMethod("run", Plugin.class, Consumer.class, Runnable.class);
                method.setAccessible(true);
                task = method
                        .invoke(scheduler, plugin, wrapped, retired);
            }
            if (task == null) {
                return cancelledTask(plugin, false);
            }
            return wrap(plugin, task, false);
        } catch (ReflectiveOperationException exception) {
            return scheduleGlobal(plugin, consumer, delay, period, repeating);
        }
    }

    private static BukkitTask scheduleRegion(Plugin plugin, Location location, Consumer<Object> consumer, long delay, long period, boolean repeating) {
        try {
            Object scheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
            Object task;
            Consumer<Object> wrapped = scheduledTask -> consumer.accept(scheduledTask);
            if (repeating) {
                Method method = scheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Location.class, Consumer.class, long.class, long.class);
                method.setAccessible(true);
                task = method.invoke(scheduler, plugin, location, wrapped, normalizeDelay(delay), period);
            } else if (delay > 0L) {
                Method method = scheduler.getClass().getMethod("runDelayed", Plugin.class, Location.class, Consumer.class, long.class);
                method.setAccessible(true);
                task = method.invoke(scheduler, plugin, location, wrapped, delay);
            } else {
                Method method = scheduler.getClass().getMethod("run", Plugin.class, Location.class, Consumer.class);
                method.setAccessible(true);
                task = method.invoke(scheduler, plugin, location, wrapped);
            }
            return wrap(plugin, task, false);
        } catch (ReflectiveOperationException exception) {
            return scheduleGlobal(plugin, consumer, delay, period, repeating);
        }
    }

    private static BukkitTask scheduleAsync(Plugin plugin, Consumer<Object> consumer, long delay, long period, boolean repeating) {
        try {
            Object scheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
            Object task;
            Consumer<Object> wrapped = scheduledTask -> consumer.accept(scheduledTask);
            if (repeating) {
                Method method = scheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, java.util.concurrent.TimeUnit.class);
                method.setAccessible(true);
                task = method
                        .invoke(scheduler, plugin, wrapped, ticksToMillis(normalizeDelay(delay)), ticksToMillis(period), java.util.concurrent.TimeUnit.MILLISECONDS);
            } else if (delay > 0L) {
                Method method = scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class, java.util.concurrent.TimeUnit.class);
                method.setAccessible(true);
                task = method
                        .invoke(scheduler, plugin, wrapped, ticksToMillis(delay), java.util.concurrent.TimeUnit.MILLISECONDS);
            } else {
                Method method = scheduler.getClass().getMethod("runNow", Plugin.class, Consumer.class);
                method.setAccessible(true);
                task = method
                        .invoke(scheduler, plugin, wrapped);
            }
            return wrap(plugin, task, true);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Failed to schedule Folia async task", exception);
        }
    }

    private static BukkitTask wrap(Plugin plugin, Object scheduledTask, boolean async) {
        int id = NEXT_ID.getAndIncrement();
        TASKS.computeIfAbsent(plugin, ignored -> new ConcurrentHashMap<>()).put(id, scheduledTask);
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if ("getTaskId".equals(name)) {
                return id;
            }
            if ("getOwner".equals(name)) {
                return plugin;
            }
            if ("isSync".equals(name)) {
                return !async;
            }
            if ("isCancelled".equals(name)) {
                return isCancelled(scheduledTask);
            }
            if ("cancel".equals(name)) {
                Map<Integer, Object> tasks = TASKS.get(plugin);
                if (tasks != null) {
                    tasks.remove(id);
                }
                cancelScheduledTask(scheduledTask);
                return null;
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("toString".equals(name)) {
                return "FoliaCompatTask{" + id + "}";
            }
            return defaultValue(method.getReturnType());
        };
        return (BukkitTask) Proxy.newProxyInstance(BukkitTask.class.getClassLoader(), new Class<?>[]{BukkitTask.class}, handler);
    }

    private static BukkitTask cancelledTask(Plugin plugin, boolean async) {
        int id = NEXT_ID.getAndIncrement();
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if ("getTaskId".equals(name)) {
                return id;
            }
            if ("getOwner".equals(name)) {
                return plugin;
            }
            if ("isSync".equals(name)) {
                return !async;
            }
            if ("isCancelled".equals(name)) {
                return true;
            }
            if ("cancel".equals(name)) {
                return null;
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("toString".equals(name)) {
                return "FoliaCompatCancelledTask{" + id + "}";
            }
            return defaultValue(method.getReturnType());
        };
        return (BukkitTask) Proxy.newProxyInstance(BukkitTask.class.getClassLoader(), new Class<?>[]{BukkitTask.class}, handler);
    }

    private static void attachBukkitRunnableTask(BukkitRunnable runnable, BukkitTask task) {
        try {
            Field field = BukkitRunnable.class.getDeclaredField("task");
            field.setAccessible(true);
            field.set(runnable, task);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Entity findCapturedEntity(Object candidate) {
        if (candidate == null) {
            return null;
        }
        if (candidate instanceof Entity entity) {
            return entity;
        }
        Class<?> type = candidate.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(candidate);
                    if (value instanceof Entity entity) {
                        return entity;
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static void cancelScheduledTask(Object scheduledTask) {
        try {
            Method method = findPublicInterfaceMethod(scheduledTask, "cancel");
            if (method == null) {
                method = scheduledTask.getClass().getMethod("cancel");
                method.setAccessible(true);
            }
            method.invoke(scheduledTask);
        } catch (ReflectiveOperationException exception) {
            // Folia may already be shutting the task down while the plugin is disabling.
            // BukkitScheduler#cancelTasks does not throw in that case, so keep that behavior.
        }
    }

    private static boolean isCancelled(Object scheduledTask) {
        try {
            Method method = findPublicInterfaceMethod(scheduledTask, "getExecutionState");
            if (method == null) {
                method = scheduledTask.getClass().getMethod("getExecutionState");
                method.setAccessible(true);
            }
            Object state = method.invoke(scheduledTask);
            return String.valueOf(state).contains("CANCELLED") || String.valueOf(state).contains("FINISHED");
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private static Method findPublicInterfaceMethod(Object target, String name) {
        for (Class<?> iface : target.getClass().getInterfaces()) {
            try {
                return iface.getMethod(name);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static boolean hasMethod(Class<?> type, String name) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static long normalizeDelay(long delay) {
        return Math.max(1L, delay);
    }

    private static long ticksToMillis(long ticks) {
        return Math.max(1L, ticks) * 50L;
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }
}
