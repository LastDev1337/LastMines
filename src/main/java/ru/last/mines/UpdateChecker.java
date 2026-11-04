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
        dev.laststudio.lib.api.LLibAPI.get().scheduler().async(plugin, () -> {
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
                        if (!latestVersion.matches("[0-9A-Za-z.\\-]{1,20}")) return;
                        @SuppressWarnings("deprecation")
                        String currentVersion = plugin.getDescription().getVersion();

                        if (isNewer(latestVersion, currentVersion)) {
                            plugin.getLogger().info("Вышла новая версия LastMines v" + latestVersion + ". Сейчас у вас стоит v" + currentVersion);
                        } else {
                            plugin.getLogger().info("Вы используете актуальную версию LastMines (" + currentVersion + ")");
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().info("Не удалось проверить обновления LastMines: " + e.getClass().getSimpleName());
            }
        });
    }

    /** Сравнение по числовым частям: "0.4" новее "0.3", но НЕ новее "0.5" (у разработчика версия может быть выше опубликованной). */
    static boolean isNewer(String latest, String current) {
        String[] a = latest.split("[.\\-]");
        String[] b = current.split("[.\\-]");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? number(a[i]) : 0;
            int y = i < b.length ? number(b[i]) : 0;
            if (x != y) return x > y;
        }
        return false;
    }

    private static int number(String part) {
        String digits = part.replaceAll("[^0-9].*", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }
}
