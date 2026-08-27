package com.dungeongates.listeners;

import com.dungeongates.DungeonGatesPlugin;
import com.dungeongates.ProgressManager;
import com.dungeongates.hooks.WorldGuardHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

public final class PlayerDeathListener implements Listener {
    
    private final DungeonGatesPlugin plugin;
    private final ProgressManager progressManager;
    private final WorldGuardHook worldGuardHook;
    
    public PlayerDeathListener(@NotNull DungeonGatesPlugin plugin) {
        this.plugin = plugin;
        this.progressManager = plugin.getProgressManager();
        this.worldGuardHook = plugin.getWorldGuardHook();
    }
    
    @EventHandler
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // Check if player was in a dungeon region
        String world = player.getWorld().getName();
        String region = worldGuardHook.getRegionAt(player.getLocation());
        if (region != null && plugin.getRoomManager().isRegisteredRegion(region, world)) {
            // Player died in dungeon - clear all progress
            progressManager.resetProgress(player.getUniqueId());
            
            String msg = plugin.getConfigManager().getPrefixedMessage("progress-reset-death");
            player.sendMessage(colorize(msg));
        }
    }
    
    private String colorize(String msg) {
        return msg.replace("&", "§");
    }
}