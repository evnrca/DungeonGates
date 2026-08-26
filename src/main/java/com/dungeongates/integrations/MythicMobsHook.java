package com.dungeongates.integrations;

import com.dungeongates.DungeonGatesPlugin;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MythicMobsHook {
    
    private final DungeonGatesPlugin plugin;
    private boolean available;
    
    public MythicMobsHook(@NotNull DungeonGatesPlugin plugin) {
        this.plugin = plugin;
        this.available = false;
    }
    
    public boolean initialize() {
        if (!MythicBukkit.installed()) {
            plugin.getLogger().severe("MythicMobs not found! This plugin requires MythicMobs.");
            return false;
        }
        
        this.available = true;
        plugin.getLogger().info("MythicMobs integration initialized successfully.");
        return true;
    }
    
    public boolean isAvailable() {
        return available;
    }
    
    public boolean isMythicMob(@NotNull Entity entity) {
        if (!available) return false;
        return MythicBukkit.inst().getMobManager().isActiveMob(entity.getUniqueId());
    }
    
    public boolean isMythicMob(@NotNull LivingEntity entity) {
        return isMythicMob((Entity) entity);
    }
    
    @Nullable
    public String getMythicMobType(@NotNull Entity entity) {
        if (!isMythicMob(entity)) return null;
        
        ActiveMob activeMob = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId());
        if (activeMob == null) return null;
        
        MythicMob mythicMob = activeMob.getType();
        return mythicMob != null ? mythicMob.getInternalName() : null;
    }
    
    public boolean isValidKill(@NotNull EntityDeathEvent event) {
        if (!available) return false;
        
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity)) return false;
        
        // Check if it's a MythicMob
        if (!isMythicMob(entity)) return false;
        
        // Check if killer is a player
        Player killer = event.getEntity().getKiller();
        if (killer == null) return false;
        
        return true;
    }
    
    @Nullable
    public String getMythicMobType(@NotNull EntityDeathEvent event) {
        return getMythicMobType(event.getEntity());
    }
    
    public boolean isPlayerKillCredit(@NotNull EntityDeathEvent event, @NotNull Player player) {
        Player killer = event.getEntity().getKiller();
        return killer != null && killer.equals(player);
    }
}