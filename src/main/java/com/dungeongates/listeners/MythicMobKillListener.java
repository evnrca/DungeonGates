package com.dungeongates.listeners;

import com.dungeongates.DungeonGatesPlugin;
import com.dungeongates.PlayerProgress;
import com.dungeongates.ProgressManager;
import com.dungeongates.Room;
import com.dungeongates.RoomProgress;
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
        
        // Get world and region where kill happened
        // First try mob's location, then fallback to killer's location
        String world = event.getEntity().getWorld().getName();
        String region = worldGuardHook.getRegionAt(event.getEntity().getLocation());
        
        // If mob died outside any region, check where the killer is
        if (region == null) {
            region = worldGuardHook.getRegionAt(killer.getLocation());
            world = killer.getWorld().getName();
        }
        
        if (region == null) return;
        
        // Check if region is a dungeon room in that world
        if (!plugin.getRoomManager().isRegisteredRegion(region, world)) {
            return;
        }
        
        // Add kill to player's progress using unique key and send progress update
        String regionKey = world + ":" + region;
        progressManager.addKillAndNotify(killer.getUniqueId(), regionKey);
    }
}