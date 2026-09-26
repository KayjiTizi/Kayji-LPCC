package com.wikmor.lpcpro.folia;

import com.wikmor.lpcpro.BukkitPlugin;
import com.wikmor.lpcpro.tablist.TabListManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class FoliaManagerCompat {
    private FoliaManagerCompat() {
    }

    public static void updateTabPlayer(TabListManager manager, Player player) {
        if (!FoliaScheduler.isFolia()) {
            manager.updatePlayer(player);
            return;
        }
        FoliaScheduler.runEntity((Plugin) BukkitPlugin.getInstance(), player, () -> manager.updatePlayer(player));
    }
}
