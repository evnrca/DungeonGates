package com.dungeongates.listeners;

import com.dungeongates.DungeonGatesPlugin;
import com.dungeongates.dungeon.ProgressManager;
import com.dungeongates.integrations.MythicMobsHook;
import com.dungeongates.integrations.WorldGuardHook;
import com.dungeongates.utils.MessageUtil;
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
    
    public MythicMobKillListener(@NotNull DungeonGatesPlugin plugin, @NotNull ProgressManager progressManager,
                                  @NotNull MythicMobsHook mythicMobsHook, @NotNull WorldGuardHook worldGuardHook) {
        this.plugin = plugin;
        this.progressManager = progressManager;
        this.mythicMobsHook = mythicMobsHook;
        this.worldGuardHook = worldGuardHook;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(@NotNull EntityDeathEvent event) {
        // Check if it's a valid MythicMob kill by a player
        if (!mythicMobsHook.isValidKill(event)) {
            return;
        }
        
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        
        // Get the MythicMob type
        String mobType = mythicMobsHook.getMythicMobType(event);
        
        // Get the room where the kill happened
        DungeonRoom room = worldGuardHook.getRoomAt(event.getEntity().getLocation());
        if (room == null) return;
        
        // Check if the killer is in the same room (or was when they killed)
        // The entity's location at death should be in the room
        if (!worldGuardHook.isInsideRegion(event.getEntity().getLocation(), room.getRegionName())) {
            return;
        }
        
        // Add kill to player's progress
        progressManager.addKill(killer.getUniqueId(), room.getName(), mobType);
    }
}