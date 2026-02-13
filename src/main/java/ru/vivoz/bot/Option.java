package ru.vivoz.bot;

public class Option {
    private final String code;
    private final String label;
    private final String value;

    public Option(String code, String label, String value) {
        this.code = code;
        this.label = label;
        this.value = value;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }
}
