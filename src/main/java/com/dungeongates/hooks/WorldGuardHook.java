package com.dungeongates.hooks;

import com.dungeongates.DungeonGatesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;

public final class WorldGuardHook {
    
    private final DungeonGatesPlugin plugin;
    private boolean initialized = false;
    
    // Reflection
    private Object worldGuardInstance;
    private Object platform;
    private Object regionContainer;
    
    private Method getInstanceMethod;
    private Method getPlatformMethod;
    private Method getRegionContainerMethod;
    private Method getRegionManagerMethod;
    private Method getApplicableRegionsMethod;
    private Method getRegionsMethod;
    private Method getRegionMethod;
    private Method getIdMethod;
    private Method getMinimumPointMethod;
    private Method getMaximumPointMethod;
    private Method addMethod;
    private Method divideMethod;
    private Method adaptMethod;
    private Method asBlockVectorMethod;
    
    public WorldGuardHook(@NotNull DungeonGatesPlugin plugin) {
        this.plugin = plugin;
    }
    
    public boolean initialize() {
        Plugin wgPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (wgPlugin == null) {
            plugin.getLogger().severe("WorldGuard not found! This plugin requires WorldGuard 7.0+");
            return false;
        }
        
        try {
            Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            getInstanceMethod = worldGuardClass.getMethod("getInstance");
            worldGuardInstance = getInstanceMethod.invoke(null);
            
            getPlatformMethod = worldGuardInstance.getClass().getMethod("getPlatform");
            platform = getPlatformMethod.invoke(worldGuardInstance);
            
            getRegionContainerMethod = platform.getClass().getMethod("getRegionContainer");
            regionContainer = getRegionContainerMethod.invoke(platform);
            
            // Load reflection classes
            Class<?> blockVector3Class = Class.forName("com.sk89q.worldedit.math.BlockVector3");
            Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Class<?> regionManagerClass = Class.forName("com.sk89q.worldguard.protection.managers.RegionManager");
            Class<?> applicableRegionSetClass = Class.forName("com.sk89q.worldguard.protection.ApplicableRegionSet");
            Class<?> protectedRegionClass = Class.forName("com.sk89q.worldguard.protection.regions.ProtectedRegion");
            
            getRegionManagerMethod = regionContainer.getClass().getMethod("get", Class.forName("com.sk89q.worldedit.world.World"));
            
            getApplicableRegionsMethod = regionManagerClass.getMethod("getApplicableRegions", blockVector3Class);
            getRegionsMethod = applicableRegionSetClass.getMethod("getRegions");
            getRegionMethod = regionManagerClass.getMethod("getRegion", String.class);
            
            getIdMethod = protectedRegionClass.getMethod("getId");
            getMinimumPointMethod = protectedRegionClass.getMethod("getMinimumPoint");
            getMaximumPointMethod = protectedRegionClass.getMethod("getMaximumPoint");
            
            addMethod = blockVector3Class.getMethod("add", blockVector3Class);
            divideMethod = blockVector3Class.getMethod("divide", int.class);
            
            adaptMethod = bukkitAdapterClass.getMethod("adapt", World.class);
            asBlockVectorMethod = bukkitAdapterClass.getMethod("asBlockVector", Location.class);
            
            initialized = true;
            plugin.getLogger().info("WorldGuard hook initialized via reflection (official API).");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize WorldGuard hook via reflection", e);
            return false;
        }
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public @Nullable Object getRegion(@NotNull String regionName) {
        if (!initialized) return null;
        
        try {
            for (World world : Bukkit.getWorlds()) {
                Object adapter = adaptMethod.invoke(null, world);
                Object manager = getRegionManagerMethod.invoke(regionContainer, adapter);
                if (manager == null) continue;
                
                Object region = getRegionMethod.invoke(manager, regionName);
                if (region != null) {
                    return region;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting region: " + regionName, e);
        }
        return null;
    }
    
    public boolean regionExists(@NotNull String regionName) {
        return getRegion(regionName) != null;
    }
    
    public @Nullable String getRegionAt(@NotNull Location location) {
        if (!initialized) return null;
        
        try {
            Object adapter = adaptMethod.invoke(null, location.getWorld());
            Object manager = getRegionManagerMethod.invoke(regionContainer, adapter);
            if (manager == null) return null;
            
            Object blockVector = asBlockVectorMethod.invoke(null, location);
            Object regionSet = getApplicableRegionsMethod.invoke(manager, blockVector);
            if (regionSet == null) return null;
            
            Collection<?> regions = (Collection<?>) getRegionsMethod.invoke(regionSet);
            if (regions == null || regions.isEmpty()) return null;
            
            // Find region with smallest area (most specific)
            Object best = null;
            int minArea = Integer.MAX_VALUE;
            
            for (Object region : regions) {
                Object minPoint = getMinimumPointMethod.invoke(region);
                Object maxPoint = getMaximumPointMethod.invoke(region);
                int area = calculateArea(minPoint, maxPoint);
                if (area < minArea) {
                    minArea = area;
                    best = region;
                }
            }
            
            if (best != null) {
                return (String) getIdMethod.invoke(best);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting region at location", e);
        }
        return null;
    }
    
    public @Nullable String getRegionAt(@NotNull Player player) {
        return getRegionAt(player.getLocation());
    }
    
    public @Nullable Location getRegionCenter(@NotNull String regionName) {
        Object region = getRegion(regionName);
        if (region == null) return null;
        
        try {
            Object minPoint = getMinimumPointMethod.invoke(region);
            Object maxPoint = getMaximumPointMethod.invoke(region);
            Object center = addMethod.invoke(minPoint, maxPoint);
            center = divideMethod.invoke(center, 2);
            
            double x = getCoord(center, "getX");
            double y = getCoord(center, "getY");
            double z = getCoord(center, "getZ");
            
            // Find world containing this region
            for (World world : Bukkit.getWorlds()) {
                Object adapter = adaptMethod.invoke(null, world);
                Object manager = getRegionManagerMethod.invoke(regionContainer, adapter);
                if (manager == null) continue;
                
                Object found = getRegionMethod.invoke(manager, regionName);
                if (found != null) {
                    return new Location(world, x, y, z);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting region center", e);
        }
        return null;
    }
    
    public boolean isInsideRegion(@NotNull Location location, @NotNull String regionName) {
        String regionAt = getRegionAt(location);
        return regionName.equals(regionAt);
    }
    
    public @NotNull List<String> getAllRegionNames() {
        Set<String> regions = new HashSet<>();
        if (!initialized) return List.of();
        
        try {
            for (World world : Bukkit.getWorlds()) {
                Object adapter = adaptMethod.invoke(null, world);
                Object manager = getRegionManagerMethod.invoke(regionContainer, adapter);
                if (manager == null) continue;
                
                Method getRegionsMapMethod = manager.getClass().getMethod("getRegions");
                Map<?, ?> regionsMap = (Map<?, ?>) getRegionsMapMethod.invoke(manager);
                if (regionsMap != null) {
                    regions.addAll(regionsMap.keySet().stream().map(Object::toString).toList());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting all region names", e);
        }
        return new ArrayList<>(regions);
    }
    
    private int calculateArea(Object minPoint, Object maxPoint) {
        try {
            double minX = getCoord(minPoint, "getX");
            double minY = getCoord(minPoint, "getY");
            double minZ = getCoord(minPoint, "getZ");
            double maxX = getCoord(maxPoint, "getX");
            double maxY = getCoord(maxPoint, "getY");
            double maxZ = getCoord(maxPoint, "getZ");
            
            return (int) Math.abs((maxX - minX) * (maxZ - minZ) * (maxY - minY));
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }
    
    private double getCoord(Object point, String methodName) {
        try {
            Method method = point.getClass().getMethod(methodName);
            return ((Number) method.invoke(point)).doubleValue();
        } catch (Exception e) {
            return 0;
        }
    }
}