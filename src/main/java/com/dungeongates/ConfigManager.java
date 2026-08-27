package com.dungeongates;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ConfigManager {
    
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;
    
    // Failed entry settings
    private String failedEntryAction = "CANCEL";
    private double velocityHorizontal = 1.5;
    private double velocityVertical = 0.4;
    
    // Denial settings
    private String deniedTitle = "&c&lROOM LOCKED!";
    private String deniedSubtitle = "&7Kill &e{remaining} &7more MythicMobs to proceed.";
    private String deniedSound = "ENTITY_VILLAGER_NO";
    private float deniedSoundVolume = 1.0f;
    private float deniedSoundPitch = 1.0f;
    
    // Progress display settings
    private boolean actionBarEnabled = true;
    private String actionBarFormat = "&eProgress: {current}/{required} MythicMobs killed";
    private boolean chatEnabled = true;
    private String chatFormat = "&8[&6Dungeon Gates&8] &eProgress: {current}/{required} MythicMobs killed";
    private int chatCooldown = 5; // seconds
    
    // Messages
    private String msgRequirementNotMet = "&cYou need {remaining} more MythicMob kills to enter {region}!";
    private String msgProgress = "&eProgress: {current}/{required} MythicMobs killed";
    private String msgCompleted = "&aRoom &e{region} &acompleted! You may now proceed.";
    private String msgPrefix = "&8[&6Dungeon Gates&8] ";
    private String msgProgressResetDeath = "&cYou died! Your dungeon progress has been reset.";
    private String msgProgressResetLogout = "&cYour dungeon progress has been reset (logout).";
    private String msgProgressResetTeleport = "&cYour dungeon progress has been reset (teleport).";
    private String msgProgressResetWorldExit = "&cYour dungeon progress has been reset (left world).";
    
    public ConfigManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void load() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Load defaults
        try (InputStream defaultStream = plugin.getResource("config.yml")) {
            if (defaultStream != null) {
                FileConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
                );
                config.setDefaults(defaults);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not load default config");
        }
        
        parseConfig();
    }
    
    private void parseConfig() {
        // Failed entry
        failedEntryAction = config.getString("denial.action", "CANCEL").toUpperCase();
        
        velocityHorizontal = config.getDouble("denial.velocity.horizontal", 1.5);
        velocityVertical = config.getDouble("denial.velocity.vertical", 0.4);
        
        // Denial settings
        deniedTitle = config.getString("denial.title", deniedTitle);
        deniedSubtitle = config.getString("denial.subtitle", deniedSubtitle);
        deniedSound = config.getString("denial.sound", deniedSound);
        deniedSoundVolume = (float) config.getDouble("denial.sound-volume", 1.0);
        deniedSoundPitch = (float) config.getDouble("denial.sound-pitch", 1.0);
        
        // Progress display settings
        actionBarEnabled = config.getBoolean("progress-display.action-bar.enabled", true);
        actionBarFormat = config.getString("progress-display.action-bar.format", actionBarFormat);
        chatEnabled = config.getBoolean("progress-display.chat.enabled", true);
        chatFormat = config.getString("progress-display.chat.format", chatFormat);
        chatCooldown = config.getInt("progress-display.chat.cooldown", 5);
        
        // Messages
        msgRequirementNotMet = config.getString("messages.requirement-not-met", msgRequirementNotMet);
        msgProgress = config.getString("messages.progress", msgProgress);
        msgCompleted = config.getString("messages.completed", msgCompleted);
        msgPrefix = config.getString("messages.prefix", msgPrefix);
        msgProgressResetDeath = config.getString("messages.progress-reset-death", msgProgressResetDeath);
        msgProgressResetLogout = config.getString("messages.progress-reset-logout", msgProgressResetLogout);
        msgProgressResetTeleport = config.getString("messages.progress-reset-teleport", msgProgressResetTeleport);
        msgProgressResetWorldExit = config.getString("messages.progress-reset-world-exit", msgProgressResetWorldExit);
    }
    
    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml");
        }
    }
    
    // Room persistence
    public @NotNull Map<String, Object> getRooms() {
        ConfigurationSection section = config.getConfigurationSection("rooms");
        if (section == null) return new HashMap<>();
        
        Map<String, Object> rooms = new HashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            rooms.put(key, value);
        }
        return rooms;
    }
    
    public void saveRoom(@NotNull String world, @NotNull String region, int requiredKills, int order) {
        String path = "rooms." + world + ":" + region;
        config.set(path, requiredKills);
        save();
    }
    
    // Legacy method
    public void saveRoom(@NotNull String region, int requiredKills, int order) {
        saveRoom("world", region, requiredKills, order);
    }
    
    public void saveRooms(@NotNull List<Room> rooms) {
        config.set("rooms", null);
        for (Room room : rooms) {
            String path = "rooms." + room.getWorld() + ":" + room.getRegion();
            config.set(path, room.getRequiredKills());
        }
        save();
    }
    
    public void removeRoom(@NotNull String world, @NotNull String region) {
        config.set("rooms." + world + ":" + region, null);
        save();
    }
    
    // Legacy method
    public void removeRoom(@NotNull String region) {
        removeRoom("world", region);
    }
    
    // Getters
    public @NotNull String getFailedEntryAction() {
        return failedEntryAction;
    }
    
    public double getVelocityHorizontal() {
        return velocityHorizontal;
    }
    
    public double getVelocityVertical() {
        return velocityVertical;
    }
    
    public @NotNull String getDeniedTitle() {
        return deniedTitle;
    }
    
    public @NotNull String getDeniedSubtitle() {
        return deniedSubtitle;
    }
    
    public @NotNull String getDeniedSound() {
        return deniedSound;
    }
    
    public float getDeniedSoundVolume() {
        return deniedSoundVolume;
    }
    
    public float getDeniedSoundPitch() {
        return deniedSoundPitch;
    }
    
    // Progress display getters
    public boolean isActionBarEnabled() {
        return actionBarEnabled;
    }
    
    public @NotNull String getActionBarFormat() {
        return actionBarFormat;
    }
    
    public boolean isChatEnabled() {
        return chatEnabled;
    }
    
    public @NotNull String getChatFormat() {
        return chatFormat;
    }
    
    public int getChatCooldown() {
        return chatCooldown;
    }
    
    public @NotNull String getMessage(@NotNull String key) {
        return switch (key) {
            case "requirement-not-met" -> msgRequirementNotMet;
            case "progress" -> msgProgress;
            case "completed" -> msgCompleted;
            case "prefix" -> msgPrefix;
            case "progress-reset-death" -> msgProgressResetDeath;
            case "progress-reset-logout" -> msgProgressResetLogout;
            case "progress-reset-teleport" -> msgProgressResetTeleport;
            case "progress-reset-world-exit" -> msgProgressResetWorldExit;
            case "denied-title" -> deniedTitle;
            case "denied-subtitle" -> deniedSubtitle;
            case "denied-sound" -> deniedSound;
            default -> "Missing message: " + key;
        };
    }
    
    public @NotNull String getPrefixedMessage(@NotNull String key) {
        return msgPrefix + getMessage(key);
    }
    
    public @NotNull FileConfiguration getConfig() {
        return config;
    }
}