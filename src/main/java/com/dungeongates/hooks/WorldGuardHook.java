package com.dungeongates.hooks;

import com.dungeongates.DungeonGatesPlugin;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

public final class WorldGuardHook {
    
    private final DungeonGatesPlugin plugin;
    private WorldGuardPlugin worldGuardPlugin;
    private RegionContainer regionContainer;
    private boolean initialized = false;
    
    public WorldGuardHook(@NotNull DungeonGatesPlugin plugin) {
        this.plugin = plugin;
    }
    
    public boolean initialize() {
        Plugin wgPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (!(wgPlugin instanceof WorldGuardPlugin)) {
            plugin.getLogger().severe("WorldGuard not found! This plugin requires WorldGuard 7.0+");
            return false;
        }
        
        this.worldGuardPlugin = (WorldGuardPlugin) wgPlugin;
        this.regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        this.initialized = true;
        
        plugin.getLogger().info("WorldGuard hook initialized (direct API).");
        return true;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public @Nullable ProtectedRegion getRegion(@NotNull String regionName, @Nullable String worldName) {
        if (!initialized) return null;
        
        if (worldName != null) {
            org.bukkit.World world = Bukkit.getWorld(worldName);
            if (world != null) {
                RegionManager manager = regionContainer.get(BukkitAdapter.adapt(world));
                if (manager != null) {
                    return manager.getRegion(regionName);
                }
            }
        } else {
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                RegionManager manager = regionContainer.get(BukkitAdapter.adapt(world));
                if (manager != null) {
                    ProtectedRegion region = manager.getRegion(regionName);
                    if (region != null) {
                        return region;
                    }
                }
            }
        }
        return null;
    }
    
    public boolean regionExists(@NotNull String regionName) {
        return getRegion(regionName, null) != null;
    }
    
    public boolean regionExists(@NotNull String regionName, @NotNull String worldName) {
        return getRegion(regionName, worldName) != null;
    }
    
    public @Nullable String getRegionAt(@NotNull Location location) {
        if (!initialized) return null;
        
        // Convert Location to BlockVector3 manually
        BlockVector3 blockVector = new BlockVector3(
            (int) location.getX(),
            (int) location.getY(),
            (int) location.getZ()
        );
        
        RegionManager manager = regionContainer.get(BukkitAdapter.adapt(location.getWorld()));
        if (manager == null) return null;
        
        ApplicableRegionSet regionSet = manager.getApplicableRegions(blockVector);
        if (regionSet == null || regionSet.size() == 0) return null;
        
        // Find region with smallest area (most specific)
        ProtectedRegion best = null;
        int minArea = Integer.MAX_VALUE;
        
        for (ProtectedRegion region : regionSet) {
            int area = calculateArea(region);
            if (area < minArea) {
                minArea = area;
                best = region;
            }
        }
        
        return best != null ? best.getId() : null;
    }
    
    public @Nullable String getRegionAt(@NotNull Player player) {
        return getRegionAt(player.getLocation());
    }
    
    public @Nullable Location getRegionCenter(@NotNull String regionName, @Nullable String worldName) {
        ProtectedRegion region = getRegion(regionName, worldName);
        if (region == null) return null;
        
        BlockVector3 minPoint = region.getMinimumPoint();
        BlockVector3 maxPoint = region.getMaximumPoint();
        BlockVector3 center = minPoint.add(maxPoint).divide(2);
        
        double x = center.getX();
        double y = center.getY();
        double z = center.getZ();
        
        // Find world containing this region
        if (worldName != null) {
            org.bukkit.World world = Bukkit.getWorld(worldName);
            if (world != null) {
                return new Location(world, x, y, z);
            }
        } else {
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                RegionManager manager = regionContainer.get(BukkitAdapter.adapt(world));
                if (manager != null && manager.getRegion(regionName) != null) {
                    return new Location(world, x, y, z);
                }
            }
        }
        return null;
    }
    
    // Legacy method
    public @Nullable Location getRegionCenter(@NotNull String regionName) {
        return getRegionCenter(regionName, null);
    }
    
    public boolean isInsideRegion(@NotNull Location location, @NotNull String regionName) {
        String regionAt = getRegionAt(location);
        return regionName.equals(regionAt);
    }
    
    public @NotNull List<String> getAllRegionNames() {
        Set<String> regions = new HashSet<>();
        if (!initialized) return List.of();
        
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            RegionManager manager = regionContainer.get(BukkitAdapter.adapt(world));
            if (manager != null) {
                regions.addAll(manager.getRegions().keySet());
            }
        }
        return new ArrayList<>(regions);
    }
    
    private int calculateArea(@NotNull ProtectedRegion region) {
        BlockVector3 minPoint = region.getMinimumPoint();
        BlockVector3 maxPoint = region.getMaximumPoint();
        
        double minX = minPoint.getX();
        double minY = minPoint.getY();
        double minZ = minPoint.getZ();
        double maxX = maxPoint.getX();
        double maxY = maxPoint.getY();
        double maxZ = maxPoint.getZ();
        
        return (int) Math.abs((maxX - minX) * (maxZ - minZ) * (maxY - minY));
    }
}