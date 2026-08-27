package com.dungeongates.listeners;

import com.dungeongates.DungeonGatesPlugin;
import com.dungeongates.ProgressManager;
import com.dungeongates.hooks.WorldGuardHook;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public final class PlayerQuitListener implements Listener {
    
    private final DungeonGatesPlugin plugin;
    private final ProgressManager progressManager;
    private final com.dungeongates.hooks.WorldGuardHook worldGuardHook;
    
    public PlayerQuitListener(@NotNull DungeonGatesPlugin plugin) {
        this.plugin = plugin;
        this.progressManager = plugin.getProgressManager();
        this.worldGuardHook = plugin.getWorldGuardHook();
    }
    
    @EventHandler
    public void onPlayerQuit(@NotNull org.bukkit.event.player.PlayerQuitEvent event) {
        // Clear progress on logout if player was in a dungeon
        // We can't easily check location on quit, so clear all progress
        // This is the safest approach
        progressManager.resetProgress(event.getPlayer().getUniqueId());
    }
}