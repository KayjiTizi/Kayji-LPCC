package com.wikmor.lpcpro.folia;

import com.wikmor.lpcpro.BukkitPlugin;
import com.wikmor.lpcpro.platform.BukkitPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.BanEntry;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.Plugin;

import java.util.Date;

public final class FoliaAudienceCompat {
    private FoliaAudienceCompat() {
    }

    public static void sendMessage(BukkitPlayer target, Component component) {
        Player player = target.getPlayer();
        if (!FoliaScheduler.isFolia() || player == null) {
            target.sendMessage(component);
            return;
        }
        FoliaScheduler.runEntity(plugin(), player, () -> target.sendMessage(component));
    }

    public static void sendPlayerListHeaderAndFooter(BukkitPlayer target, Component header, Component footer) {
        Player player = target.getPlayer();
        if (!FoliaScheduler.isFolia() || player == null) {
            target.sendPlayerListHeaderAndFooter(header, footer);
            return;
        }
        FoliaScheduler.runEntity(plugin(), player, () -> target.sendPlayerListHeaderAndFooter(header, footer));
    }

    public static void showTitle(BukkitPlayer target, Title title) {
        Player player = target.getPlayer();
        if (!FoliaScheduler.isFolia() || player == null) {
            target.showTitle(title);
            return;
        }
        FoliaScheduler.runEntity(plugin(), player, () -> target.showTitle(title));
    }

    public static void playSound(BukkitPlayer target, net.kyori.adventure.sound.Sound sound) {
        Player player = target.getPlayer();
        if (!FoliaScheduler.isFolia() || player == null) {
            target.playSound(sound);
            return;
        }
        FoliaScheduler.runEntity(plugin(), player, () -> target.playSound(sound));
    }

    public static void sendActionBar(BukkitPlayer target, Component component) {
        Player player = target.getPlayer();
        if (!FoliaScheduler.isFolia() || player == null) {
            target.sendActionBar(component);
            return;
        }
        FoliaScheduler.runEntity(plugin(), player, () -> target.sendActionBar(component));
    }

    public static void kickPlayer(Player player, String message) {
        if (!FoliaScheduler.isFolia()) {
            player.kickPlayer(message);
            return;
        }
        FoliaScheduler.runEntity(plugin(), player, () -> player.kickPlayer(message));
    }

    public static BanEntry<?> banPlayer(Player player, String reason, Date expires, String source) {
        if (!FoliaScheduler.isFolia()) {
            return player.banPlayer(reason, expires, source);
        }
        FoliaScheduler.runEntity(plugin(), player, () -> player.banPlayer(reason, expires, source));
        return null;
    }

    public static BanEntry<?> banPlayer(Player player, String reason, Date expires, String source, boolean kickIfOnline) {
        if (!FoliaScheduler.isFolia()) {
            return player.banPlayer(reason, expires, source, kickIfOnline);
        }
        FoliaScheduler.runEntity(plugin(), player, () -> player.banPlayer(reason, expires, source, kickIfOnline));
        return null;
    }

    public static BanEntry<?> ban(Player player, String reason, Date expires, String source, boolean kickIfOnline) {
        if (!FoliaScheduler.isFolia()) {
            return player.ban(reason, expires, source, kickIfOnline);
        }
        FoliaScheduler.runEntity(plugin(), player, () -> player.ban(reason, expires, source, kickIfOnline));
        return null;
    }

    public static boolean teleport(Player player, Location location) {
        if (!FoliaScheduler.isFolia()) {
            return player.teleport(location);
        }
        return FoliaEntityCompat.teleport(player, location);
    }

    public static InventoryView openInventory(Player player, Inventory inventory) {
        if (!FoliaScheduler.isFolia()) {
            return player.openInventory(inventory);
        }
        FoliaScheduler.runEntity(plugin(), player, () -> player.openInventory(inventory));
        return null;
    }

    public static void closeInventory(Player player) {
        if (!FoliaScheduler.isFolia()) {
            player.closeInventory();
            return;
        }
        FoliaScheduler.runEntity(plugin(), player, player::closeInventory);
    }

    public static boolean performCommand(Player player, String command) {
        if (!FoliaScheduler.isFolia()) {
            return player.performCommand(command);
        }
        FoliaScheduler.runEntity(plugin(), player, () -> player.performCommand(command));
        return true;
    }

    public static void chat(Player player, String message) {
        if (!FoliaScheduler.isFolia()) {
            player.chat(message);
            return;
        }
        FoliaScheduler.runEntity(plugin(), player, () -> player.chat(message));
    }

    public static void playSound(Player player, Location location, Sound sound, float volume, float pitch) {
        if (!FoliaScheduler.isFolia()) {
            player.playSound(location, sound, volume, pitch);
            return;
        }
        FoliaScheduler.runEntity(plugin(), player, () -> player.playSound(location, sound, volume, pitch));
    }

    public static void sendPluginMessage(Player player, Plugin source, String channel, byte[] message) {
        if (!FoliaScheduler.isFolia()) {
            player.sendPluginMessage(source, channel, message);
            return;
        }
        FoliaScheduler.runEntity(plugin(), player, () -> player.sendPluginMessage(source, channel, message));
    }

    private static Plugin plugin() {
        return BukkitPlugin.getInstance();
    }
}
