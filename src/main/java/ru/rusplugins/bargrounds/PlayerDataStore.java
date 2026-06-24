package ru.rusplugins.bargrounds;

import cn.nukkit.Player;
import cn.nukkit.utils.Config;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class PlayerDataStore {

    private final Config config;

    public PlayerDataStore(File dataFolder) {
        this.config = new Config(new File(dataFolder, "playerdata.yml"), Config.YAML);
    }

    public int getKills(Player player) {
        return config.getInt(path(player, "kills"), 0);
    }

    public int getDeaths(Player player) {
        return config.getInt(path(player, "deaths"), 0);
    }

    public void addKill(Player player) {
        String path = path(player, "kills");
        config.set(path, config.getInt(path, 0) + 1);
    }

    public void addDeath(Player player) {
        String path = path(player, "deaths");
        config.set(path, config.getInt(path, 0) + 1);
    }

    public void save() {
        config.save();
    }

    public Map<String, Integer> getCompactStats(Player player) {
        Map<String, Integer> map = new LinkedHashMap<String, Integer>();
        map.put("kills", getKills(player));
        map.put("deaths", getDeaths(player));
        return map;
    }

    private String path(Player player, String key) {
        return "players." + player.getName().toLowerCase() + "." + key;
    }
}