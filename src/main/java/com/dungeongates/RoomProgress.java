package com.dungeongates;

public final class RoomProgress {
    
    private int kills = 0;
    private boolean completed = false;
    
    public int getKills() {
        return kills;
    }
    
    public void addKill() {
        kills++;
    }
    
    public void setKills(int kills) {
        this.kills = kills;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    public void reset() {
        kills = 0;
        completed = false;
    }
}