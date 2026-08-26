package com.dungeongates.integrations;

import com.dungeongates.DungeonGatesPlugin;
import com.dungeongates.dungeon.DungeonRoom;
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
import org.bukkit.World;
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
    
    public WorldGuardHook(@NotNull DungeonGatesPlugin plugin) {
        this.plugin = plugin;
    }
    
    public boolean initialize() {
        Plugin wgPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (!(wgPlugin instanceof WorldGuardPlugin)) {
            plugin.getLogger().severe("WorldGuard not found! This plugin requires WorldGuard.");
            return false;
        }
        
        this.worldGuardPlugin = (WorldGuardPlugin) wgPlugin;
        this.regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        
        plugin.getLogger().info("WorldGuard integration initialized successfully.");
        return true;
    }
    
    public @Nullable ProtectedRegion getRegion(@NotNull String regionName) {
        if (regionContainer == null) return null;
        
        for (World world : Bukkit.getWorlds()) {
            RegionManager manager = regionContainer.get(BukkitAdapter.adapt(world));
            if (manager != null) {
                ProtectedRegion region = manager.getRegion(regionName);
                if (region != null) {
                    return region;
                }
            }
        }
        return null;
    }
    
    public boolean regionExists(@NotNull String regionName) {
        return getRegion(regionName) != null;
    }
    
    public @Nullable ProtectedRegion getRegionAt(@NotNull Location location) {
        if (regionContainer == null) return null;
        
        RegionManager manager = regionContainer.get(BukkitAdapter.adapt(location.getWorld()));
        if (manager == null) return null;
        
        ApplicableRegionSet set = manager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
        if (set == null || set.size() == 0) return null;
        
        // Return the region with highest priority (smallest area typically)
        return set.getRegions().stream()
                .min(Comparator.comparingInt(r -> r.getArea()))
                .orElse(null);
    }
    
    public @Nullable DungeonRoom getRoomAt(@NotNull Location location) {
        ProtectedRegion region = getRegionAt(location);
        if (region == null) return null;
        
        return plugin.getRoomManager().getRoomByRegion(region.getId());
    }
    
    public @Nullable DungeonRoom getRoomAt(@NotNull Player player) {
        return getRoomAt(player.getLocation());
    }
    
    public @Nullable Location getRegionCenter(@NotNull String regionName) {
        ProtectedRegion region = getRegion(regionName);
        if (region == null) return null;
        
        BlockVector3 center = region.getMaximumPoint().add(region.getMinimumPoint()).divide(2);
        World world = Bukkit.getWorlds().stream()
                .filter(w -> regionContainer.get(BukkitAdapter.adapt(w)) != null)
                .findFirst()
                .orElse(null);
        
        if (world == null) return null;
        
        return BukkitAdapter.adapt(world, center);
    }
    
    public @Nullable Location getRegionCenter(@NotNull ProtectedRegion region) {
        BlockVector3 center = region.getMaximumPoint().add(region.getMinimumPoint()).divide(2);
        World world = Bukkit.getWorlds().stream()
                .filter(w -> regionContainer.get(BukkitAdapter.adapt(w)) != null)
                .findFirst()
                .orElse(null);
        
        if (world == null) return null;
        
        return BukkitAdapter.adapt(world, center);
    }
    
    public boolean isInsideRegion(@NotNull Location location, @NotNull String regionName) {
        ProtectedRegion region = getRegion(regionName);
        if (region == null) return false;
        
        RegionManager manager = regionContainer.get(BukkitAdapter.adapt(location.getWorld()));
        if (manager == null) return false;
        
        ApplicableRegionSet set = manager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
        return set != null && set.contains(region);
    }
    
    public @NotNull List<String> getAllRegionNames() {
        Set<String> regions = new HashSet<>();
        if (regionContainer == null) return List.of();
        
        for (World world : Bukkit.getWorlds()) {
            RegionManager manager = regionContainer.get(BukkitAdapter.adapt(world));
            if (manager != null) {
                regions.addAll(manager.getRegions().keySet());
            }
        }
        return new ArrayList<>(regions);
    }
    
    public void validateRooms(@NotNull Collection<DungeonRoom> rooms) {
        for (DungeonRoom room : rooms) {
            ProtectedRegion region = getRegion(room.getRegionName());
            if (region != null) {
                room.setRegion(region);
            } else {
                plugin.getLogger().warning("WorldGuard region '" + room.getRegionName() + "' not found for room '" + room.getName() + "'");
                room.setRegion(null);
            }
        }
    }
}