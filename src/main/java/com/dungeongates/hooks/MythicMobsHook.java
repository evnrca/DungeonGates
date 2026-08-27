package com.dungeongates.hooks;

import com.dungeongates.DungeonGatesPlugin;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.mobs.MobExecutor;
import io.lumine.mythic.api.mobs.MythicMob;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

public final class MythicMobsHook {
    
    private final DungeonGatesPlugin plugin;
    private MobExecutor mobExecutor;
    private boolean initialized = false;
    
    public MythicMobsHook(@NotNull DungeonGatesPlugin plugin) {
        this.plugin = plugin;
    }
    
    public boolean initialize() {
        if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
            plugin.getLogger().severe("MythicMobs not found! This plugin requires MythicMobs 5.6+");
            return false;
        }
        
        try {
            this.mobExecutor = MythicBukkit.inst().getMobManager();
            if (this.mobExecutor == null) {
                plugin.getLogger().severe("MythicMobs MobExecutor not available!");
                return false;
            }
            this.initialized = true;
            plugin.getLogger().info("MythicMobs hook initialized (direct API).");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize MythicMobs hook (direct API)", e);
            return false;
        }
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public boolean isMythicMob(@NotNull Entity entity) {
        if (!initialized) return false;
        return mobExecutor.isActiveMob(entity.getUniqueId());
    }
    
    public boolean isMythicMob(@NotNull LivingEntity entity) {
        return isMythicMob((Entity) entity);
    }
    
    @Nullable
    public String getMythicMobType(@NotNull Entity entity) {
        if (!isMythicMob(entity)) return null;
        
        ActiveMob activeMob = mobExecutor.getActiveMob(entity.getUniqueId()).orElse(null);
        if (activeMob == null) return null;
        
        MythicMob mythicMob = activeMob.getType();
        return mythicMob != null ? mythicMob.getInternalName() : null;
    }
    
    public boolean isValidKill(@NotNull EntityDeathEvent event) {
        if (!initialized) return false;
        
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity)) return false;
        
        if (!isMythicMob(entity)) return false;
        
        LivingEntity livingEntity = (LivingEntity) entity;
        EntityDamageEvent lastDamage = livingEntity.getLastDamageCause();
        if (lastDamage == null || !(lastDamage instanceof EntityDamageByEntityEvent)) return false;
        
        EntityDamageByEntityEvent damageByEntity = (EntityDamageByEntityEvent) lastDamage;
        Entity killer = damageByEntity.getDamager();
        return killer instanceof Player;
    }
    
    @Nullable
    public String getMythicMobType(@NotNull EntityDeathEvent event) {
        return getMythicMobType(event.getEntity());
    }
    
    public boolean isPlayerKillCredit(@NotNull EntityDeathEvent event, @NotNull Player player) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity)) return false;
        
        LivingEntity livingEntity = (LivingEntity) entity;
        EntityDamageEvent lastDamage = livingEntity.getLastDamageCause();
        if (lastDamage == null || !(lastDamage instanceof EntityDamageByEntityEvent)) return false;
        
        EntityDamageByEntityEvent damageByEntity = (EntityDamageByEntityEvent) lastDamage;
        Entity killer = damageByEntity.getDamager();
        return killer != null && killer.equals(player);
    }
}