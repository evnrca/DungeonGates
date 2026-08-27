package com.dungeongates.hooks;

import com.dungeongates.DungeonGatesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.logging.Level;

public final class MythicMobsHook {
    
    private final DungeonGatesPlugin plugin;
    private boolean initialized = false;
    
    // Reflection
    private Object mythicBukkitInstance;
    private Object mobManager;
    
    private Method instMethod;
    private Method getMobManagerMethod;
    private Method isActiveMobMethod;
    private Method getActiveMobMethod;
    private Method getTypeMethod;
    private Method getInternalNameMethod;
    
    public MythicMobsHook(@NotNull DungeonGatesPlugin plugin) {
        this.plugin = plugin;
    }
    
    public boolean initialize() {
        if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
            plugin.getLogger().warning("MythicMobs not found! Kill tracking disabled.");
            return false;
        }
        
        try {
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            instMethod = mythicBukkitClass.getMethod("inst");
            mythicBukkitInstance = instMethod.invoke(null);
            
            getMobManagerMethod = mythicBukkitInstance.getClass().getMethod("getMobManager");
            mobManager = getMobManagerMethod.invoke(mythicBukkitInstance);
            
            isActiveMobMethod = mobManager.getClass().getMethod("isActiveMob", java.util.UUID.class);
            getActiveMobMethod = mobManager.getClass().getMethod("getActiveMob", java.util.UUID.class);
            getTypeMethod = Class.forName("io.lumine.mythic.core.mobs.ActiveMob").getMethod("getType");
            getInternalNameMethod = Class.forName("io.lumine.mythic.api.mobs.MythicMob").getMethod("getInternalName");
            
            initialized = true;
            plugin.getLogger().info("MythicMobs hook initialized via reflection.");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to initialize MythicMobs hook", e);
            return false;
        }
    }
    
    public boolean isAvailable() {
        return initialized;
    }
    
    public boolean isMythicMob(@NotNull Entity entity) {
        if (!initialized) return false;
        try {
            return (Boolean) isActiveMobMethod.invoke(mobManager, entity.getUniqueId());
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean isMythicMob(@NotNull LivingEntity entity) {
        return isMythicMob((Entity) entity);
    }
    
    @Nullable
    public String getMythicMobType(@NotNull Entity entity) {
        if (!isMythicMob(entity)) return null;
        try {
            Object activeMob = getActiveMobMethod.invoke(mobManager, entity.getUniqueId());
            if (activeMob == null) return null;
            Object mythicMob = getTypeMethod.invoke(activeMob);
            if (mythicMob == null) return null;
            return (String) getInternalNameMethod.invoke(mythicMob);
        } catch (Exception e) {
            return null;
        }
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