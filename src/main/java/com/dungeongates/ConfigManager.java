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
    
    // Settings
    private String failedEntryAction = "VELOCITY";
    private double velocityHorizontal = 1.5;
    private double velocityVertical = 0.4;
    
    // Messages
    private String msgRequirementNotMet = "&cYou need {remaining} more MythicMob kills!";
    private String msgProgress = "&eProgress: {current}/{required}";
    private String msgCompleted = "&aRoom requirement completed!";
    private String msgPrefix = "&8[&6Dungeon Gates&8] ";
    
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
        failedEntryAction = config.getString("failed-entry.action", "VELOCITY").toUpperCase();
        
        velocityHorizontal = config.getDouble("failed-entry.velocity.horizontal", 1.5);
        velocityVertical = config.getDouble("failed-entry.velocity.vertical", 0.4);
        
        // Messages
        msgRequirementNotMet = config.getString("messages.requirement-not-met", msgRequirementNotMet);
        msgProgress = config.getString("messages.progress", msgProgress);
        msgCompleted = config.getString("messages.completed", msgCompleted);
        msgPrefix = config.getString("messages.prefix", msgPrefix);
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
    
    public void saveRoom(@NotNull String region, int requiredKills, int order) {
        String path = "rooms." + region;
        config.set(path, requiredKills);
        save();
    }
    
    public void saveRooms(@NotNull List<Room> rooms) {
        config.set("rooms", null);
        for (Room room : rooms) {
            String path = "rooms." + room.getRegion();
            config.set(path, room.getRequiredKills());
        }
        save();
    }
    
    public void removeRoom(@NotNull String region) {
        config.set("rooms." + region, null);
        save();
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
    
    public @NotNull String getMessage(@NotNull String key) {
        return switch (key) {
            case "requirement-not-met" -> msgRequirementNotMet;
            case "progress" -> msgProgress;
            case "completed" -> msgCompleted;
            case "prefix" -> msgPrefix;
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