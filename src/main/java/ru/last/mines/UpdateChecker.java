package ru.last.mines;

import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

public class UpdateChecker {

    private final LastMines plugin;
    private final String versionUrl = "https://raw.githubusercontent.com/LastDev1337/LastMines/refs/heads/master/VERSION";

    public UpdateChecker(LastMines plugin) { this.plugin = plugin; }

    public void check() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URI uri = URI.create(versionUrl);
                HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() == 200) {
                    String latestVersion;
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        latestVersion = reader.readLine();
                    }
                    if (latestVersion != null) {
                        latestVersion = latestVersion.trim();
                        @SuppressWarnings("deprecation")
                        String currentVersion = plugin.getDescription().getVersion();

                        if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                            plugin.getLogger().info("Вышла новая версия LastMines v" + latestVersion + ". Сейчас у вас стоит v" + currentVersion);
                        } else {
                            plugin.getLogger().info("Вы используете актуальную версию LastMines (" + currentVersion + ")");
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().info("Не удалось проверить обновления LastMines:");
                e.fillInStackTrace();
            }
        });
    }
}
