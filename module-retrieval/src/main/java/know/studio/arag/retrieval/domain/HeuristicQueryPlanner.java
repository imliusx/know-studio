package know.studio.arag.retrieval.domain;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;

@Component
public class HeuristicQueryPlanner implements QueryPlanner {

    private static final int MAX_QUERIES = 3;

    @Override
    public List<String> plan(String question) {
        LinkedHashSet<String> queries = new LinkedHashSet<>();
        String normalized = question.replaceAll("[?？!！]+$", "").trim();
        if (!normalized.isBlank()) {
            queries.add(normalized);
        }
        for (String part : normalized.split("(?:以及|并且|同时| and |;|；)")) {
            String value = part.trim();
            if (value.length() >= 4) {
                queries.add(value);
            }
            if (queries.size() >= MAX_QUERIES) {
                break;
            }
        }
        return queries.stream().limit(MAX_QUERIES).toList();
    }
}
