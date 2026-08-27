package com.dungeongates;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Room {
    
    private final @NotNull String region;
    private final @NotNull String world;
    private final int requiredKills;
    private int order;
    
    public Room(@NotNull String region, @NotNull String world, int requiredKills) {
        this.region = region;
        this.world = world;
        this.requiredKills = requiredKills;
        this.order = -1;
    }
    
    // Backwards compatibility constructor
    public Room(@NotNull String region, int requiredKills) {
        this(region, "world", requiredKills);
    }
    
    public @NotNull String getRegion() {
        return region;
    }
    
    public @NotNull String getWorld() {
        return world;
    }
    
    public int getRequiredKills() {
        return requiredKills;
    }
    
    public int getOrder() {
        return order;
    }
    
    public void setOrder(int order) {
        this.order = order;
    }
    
    public @NotNull String getUniqueKey() {
        return world + ":" + region;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Room that = (Room) obj;
        return region.equals(that.region) && world.equals(that.world);
    }
    
    @Override
    public int hashCode() {
        return getUniqueKey().hashCode();
    }
    
    @Override
    public String toString() {
        return "Room{region='" + region + "', world='" + world + "', requiredKills=" + requiredKills + ", order=" + order + "}";
    }
}