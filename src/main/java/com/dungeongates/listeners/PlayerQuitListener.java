package com.dungeongates.listeners;

import com.dungeongates.DungeonGatesPlugin;
import com.dungeongates.dungeon.ProgressManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public final class PlayerQuitListener implements Listener {
    
    private final DungeonGatesPlugin plugin;
    private final ProgressManager progressManager;
    
    public PlayerQuitListener(@NotNull DungeonGatesPlugin plugin, @NotNull ProgressManager progressManager) {
        this.plugin = plugin;
        this.progressManager = progressManager;
    }
    
    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        progressManager.handlePlayerQuit(event);
    }
}