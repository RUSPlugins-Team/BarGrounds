package ru.rusplugins.bargrounds;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Method;

public class PluginHooks {

    private final BarGroundsPlugin plugin;

    public static class HookResult {
        private final boolean available;
        private final boolean safe;
        private final String value;

        public HookResult(boolean available, boolean safe, String value) {
            this.available = available;
            this.safe = safe;
            this.value = value;
        }

        public boolean isAvailable() {
            return available;
        }

        public boolean isSafe() {
            return safe;
        }

        public String getValue() {
            return value;
        }
    }

    public PluginHooks(BarGroundsPlugin plugin) {
        this.plugin = plugin;
    }

    public HookResult readRank(Player player) {
        Plugin rankPlugin = getIntegrationPlugin("rank", "rank-plugin", "LuckPerms");
        if (rankPlugin == null || !rankPlugin.isEnabled()) {
            return new HookResult(false, false, null);
        }

        try {
            Method method = rankPlugin.getClass().getMethod("getPrimaryGroup", Player.class);
            Object value = method.invoke(rankPlugin, player);
            if (value != null) {
                return new HookResult(true, true, String.valueOf(value));
            }
            return new HookResult(true, true, "Available");
        } catch (Exception ignored) {
            return new HookResult(true, false, "Available");
        }
    }

    public HookResult readClan(Player player) {
        Plugin clanPlugin = getIntegrationPlugin("clan", "clan-plugin", "Clans");
        if (clanPlugin == null || !clanPlugin.isEnabled()) {
            return new HookResult(false, false, null);
        }

        return new HookResult(true, true, "Available");
    }

    public HookResult readCoins(Player player) {
        Plugin economyApi = getIntegrationPlugin("economy", null, "EconomyAPI");
        if (economyApi == null || !economyApi.isEnabled()) {
            return new HookResult(false, false, null);
        }

        try {
            Class<?> clazz = Class.forName("me.onebone.economyapi.EconomyAPI");
            Method getInstance = clazz.getMethod("getInstance");
            Object instance = getInstance.invoke(null);
            Method myMoney = clazz.getMethod("myMoney", Player.class);
            Object money = myMoney.invoke(instance, player);
            String value = String.valueOf(Math.round(Double.parseDouble(String.valueOf(money))));
            return new HookResult(true, true, value);
        } catch (Exception ignored) {
            return new HookResult(true, false, "0");
        }
    }

    public HookResult readRubies() {
        Plugin rubiesPlugin = getIntegrationPlugin("rubies", "rubies-plugin", "Rubies");
        if (rubiesPlugin == null || !rubiesPlugin.isEnabled()) {
            return new HookResult(false, false, null);
        }
        return new HookResult(true, true, "Available");
    }

    public HookResult readDonateCoins() {
        Plugin donatePlugin = getIntegrationPlugin("donate-coins", "donate-coins-plugin", "DonateCoins");
        if (donatePlugin == null || !donatePlugin.isEnabled()) {
            return new HookResult(false, false, null);
        }
        return new HookResult(true, true, "Available");
    }

    public List<String> buildStartupReport() {
        List<String> lines = new ArrayList<String>();
        lines.add(reportLine("Economy", getIntegrationPlugin("economy", null, "EconomyAPI"), true));
        lines.add(reportLine("Rank", getIntegrationPlugin("rank", "rank-plugin", "LuckPerms"), false));
        lines.add(reportLine("Clan", getIntegrationPlugin("clan", "clan-plugin", "Clans"), false));
        lines.add(reportLine("Rubies", getIntegrationPlugin("rubies", "rubies-plugin", "Rubies"), false));
        lines.add(reportLine("DonateCoins", getIntegrationPlugin("donate-coins", "donate-coins-plugin", "DonateCoins"), false));
        return lines;
    }

    private String reportLine(String key, Plugin foundPlugin, boolean verifySafeReflection) {
        if (foundPlugin == null || !foundPlugin.isEnabled()) {
            return "[MISSING] " + key + " integration plugin not found (feature disabled)";
        }

        if (verifySafeReflection && !isEconomyReflectionSafe()) {
            return "[WARN] " + key + " found, but safe hook check failed";
        }

        return "[OK] " + key + " found, safe integration enabled";
    }

    private boolean isEconomyReflectionSafe() {
        try {
            Class<?> clazz = Class.forName("me.onebone.economyapi.EconomyAPI");
            clazz.getMethod("getInstance");
            clazz.getMethod("myMoney", Player.class);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private Plugin getIntegrationPlugin(String integrationKey, String legacyKey, String defaultName) {
        String fromSection = plugin.getConfig().getString("integrations." + integrationKey, defaultName);
        String pluginName = fromSection;
        if (pluginName == null || pluginName.trim().isEmpty()) {
            pluginName = defaultName;
        }

        if (legacyKey != null) {
            String legacy = plugin.getConfig().getString(legacyKey, pluginName);
            if (legacy != null && !legacy.trim().isEmpty()) {
                pluginName = legacy;
            }
        }

        return Server.getInstance().getPluginManager().getPlugin(pluginName);
    }
}