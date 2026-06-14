package ru.last.mines.api;

public class LastMinesProvider {
    private static LastMinesAPI api;

    public static LastMinesAPI getApi() { return api; }

    public static void setApi(LastMinesAPI api) {
        if (LastMinesProvider.api != null) {
            throw new UnsupportedOperationException("API is already registered!");
        }
        LastMinesProvider.api = api;
    }
}
