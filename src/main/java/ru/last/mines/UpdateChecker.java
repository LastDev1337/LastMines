package ru.last.mines;

import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

public class UpdateChecker {

    private final LastMines plugin;
    private final String versionUrl = "https://raw.githubusercontent.com/LastDev1337/LastMines/refs/heads/master/VERSION";

    public UpdateChecker(LastMines plugin) { this.plugin = plugin; }

    public void check() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                java.net.URI uri = java.net.URI.create(versionUrl);
                HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String latestVersion = reader.readLine();
                    if (latestVersion != null) {
                        latestVersion = latestVersion.trim();
                        @SuppressWarnings("deprecation")
                        String currentVersion = plugin.getDescription().getVersion();
                        
                        if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                            plugin.getLogger().warning("==============================================================");
                            plugin.getLogger().warning("Доступна новая версия LastMines: " + latestVersion);
                            plugin.getLogger().warning("Ваша текущая версия: " + currentVersion);
                            plugin.getLogger().warning("Скачайте обновление, чтобы использовать плагин на максимум!");
                            plugin.getLogger().warning("==============================================================");
                        } else {
                            plugin.getLogger().info("Вы используете актуальную версию LastMines (" + currentVersion + ")");
                        }
                    }
                    reader.close();
                }
            } catch (Exception e) { plugin.getLogger().warning("Не удалось проверить обновления LastMines: " + e.getMessage()); }
        });
    }
}
