package com.dungeongates;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Room {
    
    private final @NotNull String region;
    private final int requiredKills;
    private int order;
    
    public Room(@NotNull String region, int requiredKills) {
        this.region = region;
        this.requiredKills = requiredKills;
        this.order = -1;
    }
    
    public @NotNull String getRegion() {
        return region;
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
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Room that = (Room) obj;
        return region.equals(that.region);
    }
    
    @Override
    public int hashCode() {
        return region.hashCode();
    }
    
    @Override
    public String toString() {
        return "Room{region='" + region + "', requiredKills=" + requiredKills + ", order=" + order + "}";
    }
}