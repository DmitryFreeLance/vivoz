package ru.vivoz.bot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Session {
    private final OrderType type;
    private int step;
    private final Map<String, String> answers = new LinkedHashMap<>();

    public Session(OrderType type) {
        this.type = type;
        this.step = 0;
    }

    public OrderType getType() {
        return type;
    }

    public int getStep() {
        return step;
    }

    public Map<String, String> getAnswers() {
        return answers;
    }

    public Question currentQuestion() {
        List<Question> questions = OrderFlow.questionsFor(type);
        if (step < 0 || step >= questions.size()) {
            return null;
        }
        return questions.get(step);
    }

    public boolean advance() {
        step++;
        return step < OrderFlow.questionsFor(type).size();
    }
}
