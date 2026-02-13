package ru.vivoz.bot;

public enum OrderType {
    HOME_MOVE("Домашний переезд"),
    OFFICE_MOVE("Офисный переезд"),
    LOADERS_ONLY("Только грузчики"),
    GAZELLE_ONLY("Только газель");

    private final String title;

    OrderType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
