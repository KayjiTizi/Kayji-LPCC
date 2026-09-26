package com.wikmor.lpcpro.listener;

import com.wikmor.lpcpro.BukkitPlugin;
import com.wikmor.lpcpro.channel.ChatMessage;
import com.wikmor.lpcpro.folia.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class FoliaChatCompat {
    private FoliaChatCompat() {
    }

    public static void handle(ChatMessage message) {
        CommandSender sender = message.getSender();
        if (!FoliaScheduler.isFolia() || !(sender instanceof Player player)) {
            ChatHandler.handle(message);
            return;
        }
        boolean originalCancelled = message.isCancelled();
        message.setCancelled(true);
        FoliaScheduler.runEntity((Plugin) BukkitPlugin.getInstance(), player, () -> {
            message.setCancelled(originalCancelled);
            ChatHandler.handle(message);
            if (!message.isCancelled()) {
                String consoleFormat = message.getConsoleFormat();
                if (consoleFormat != null) {
                    Bukkit.getConsoleSender().sendMessage(consoleFormat);
                } else {
                    Bukkit.getConsoleSender().sendMessage("<" + message.getSender().getName() + "> " + message.getMessage());
                }
            }
        });
    }
}
