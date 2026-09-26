package com.wikmor.lpcpro.folia;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class FoliaServerCompat {
    private FoliaServerCompat() {
    }

    public static boolean dispatchCommand(CommandSender sender, String command) {
        if (!FoliaScheduler.isFolia()) {
            return Bukkit.dispatchCommand(sender, command);
        }
        Plugin plugin = JavaPlugin.getProvidingPlugin(FoliaServerCompat.class);
        FoliaScheduler.runGlobal(plugin, () -> Bukkit.dispatchCommand(sender, command));
        return true;
    }
}
