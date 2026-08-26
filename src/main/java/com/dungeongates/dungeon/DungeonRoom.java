package com.dungeongates.dungeon;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DungeonRoom {
    
    private final String name;
    private final String regionName;
    private final int requiredKills;
    private transient ProtectedRegion region;
    private int order;
    
    public DungeonRoom(@NotNull String name, @NotNull String regionName, int requiredKills) {
        this.name = name;
        this.regionName = regionName;
        this.requiredKills = requiredKills;
        this.order = -1;
    }
    
    public @NotNull String getName() {
        return name;
    }
    
    public @NotNull String getRegionName() {
        return regionName;
    }
    
    public int getRequiredKills() {
        return requiredKills;
    }
    
    public @Nullable ProtectedRegion getRegion() {
        return region;
    }
    
    public void setRegion(@Nullable ProtectedRegion region) {
        this.region = region;
    }
    
    public int getOrder() {
        return order;
    }
    
    public void setOrder(int order) {
        this.order = order;
    }
    
    public boolean hasRegion() {
        return region != null;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DungeonRoom that = (DungeonRoom) obj;
        return name.equals(that.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    @Override
    public String toString() {
        return "DungeonRoom{name='" + name + "', region='" + regionName + "', requiredKills=" + requiredKills + ", order=" + order + "}";
    }
}