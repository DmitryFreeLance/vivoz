package ru.vivoz.bot;

import java.util.Collections;
import java.util.List;

public class Question {
    private final String key;
    private final String label;
    private final String text;
    private final List<Option> options;

    public Question(String key, String label, String text) {
        this(key, label, text, Collections.emptyList());
    }

    public Question(String key, String label, String text, List<Option> options) {
        this.key = key;
        this.label = label;
        this.text = text;
        this.options = options == null ? Collections.emptyList() : options;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public String getText() {
        return text;
    }

    public List<Option> getOptions() {
        return options;
    }

    public boolean hasOptions() {
        return options != null && !options.isEmpty();
    }
}
