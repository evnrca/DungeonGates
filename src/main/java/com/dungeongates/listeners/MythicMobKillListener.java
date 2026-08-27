package com.dungeongates.listeners;

import com.dungeongates.DungeonGatesPlugin;
import com.dungeongates.ProgressManager;
import com.dungeongates.hooks.MythicMobsHook;
import com.dungeongates.hooks.WorldGuardHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;

public final class MythicMobKillListener implements Listener {
    
    private final DungeonGatesPlugin plugin;
    private final ProgressManager progressManager;
    private final MythicMobsHook mythicMobsHook;
    private final WorldGuardHook worldGuardHook;
    
    public MythicMobKillListener(@NotNull DungeonGatesPlugin plugin) {
        this.plugin = plugin;
        this.progressManager = plugin.getProgressManager();
        this.mythicMobsHook = plugin.getMythicMobsHook();
        this.worldGuardHook = plugin.getWorldGuardHook();
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(@NotNull EntityDeathEvent event) {
        // Check if valid MythicMob kill by a player
        if (!mythicMobsHook.isValidKill(event)) {
            return;
        }
        
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        
        // Get region where kill happened
        String region = worldGuardHook.getRegionAt(event.getEntity().getLocation());
        if (region == null) return;
        
        // Check if region is a dungeon room
        if (!plugin.getRoomManager().isRegisteredRegion(region)) {
            return;
        }
        
        // Add kill to player's progress
        progressManager.addKill(killer.getUniqueId(), region);
    }
}