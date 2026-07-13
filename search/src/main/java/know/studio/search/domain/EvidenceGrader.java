package know.studio.search.domain;

import know.studio.search.api.EvidenceLevel;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EvidenceGrader {

    private static final double MIN_LEXICAL_COVERAGE = 0.34;
    private static final Pattern ASCII_TERM_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_-]+");
    private static final Set<String> ENGLISH_STOP_TERMS = Set.of(
            "a", "an", "are", "can", "do", "does", "how", "is", "please", "the", "what", "why"
    );
    private static final Pattern QUESTION_WORDS = Pattern.compile(
            "如何|怎么|怎样|什么|为什么|是否|请问|分别|产生的?|应该|可以|需要|"
                    + "介绍一下|解释一下|解释|说明|查询|帮我|一下|的|和|与|及|是|吗|呢"
    );

    public EvidenceLevel grade(String question, List<FusedCandidate> candidates) {
        if (candidates.isEmpty()) {
            return EvidenceLevel.NONE;
        }
        FusedCandidate top = candidates.getFirst();
        if (top.rerankScore() != null) {
            if (top.rerankScore() >= 0.72) {
                return EvidenceLevel.SUFFICIENT;
            }
            if (top.rerankScore() >= 0.45) {
                return EvidenceLevel.PARTIAL;
            }
            if (top.rerankScore() >= 0.20) {
                return EvidenceLevel.WEAK;
            }
            return EvidenceLevel.NONE;
        }
        if (lexicalCoverage(question, candidates) < MIN_LEXICAL_COVERAGE) {
            return EvidenceLevel.NONE;
        }
        long hybridHits = candidates.stream()
                .filter(candidate -> candidate.sources().size() > 1)
                .count();
        if (hybridHits >= 2 || hybridHits == 1 && candidates.size() >= 3) {
            return EvidenceLevel.SUFFICIENT;
        }
        if (hybridHits == 1) {
            return EvidenceLevel.PARTIAL;
        }
        int supportCount = candidates.stream().mapToInt(FusedCandidate::supportCount).sum();
        return supportCount >= 2 ? EvidenceLevel.WEAK : EvidenceLevel.NONE;
    }

    static double lexicalCoverage(String question, List<FusedCandidate> candidates) {
        Set<String> terms = queryTerms(question);
        if (terms.isEmpty()) {
            return 0.0;
        }
        String evidenceText = candidates.stream()
                .limit(5)
                .map(FusedCandidate::text)
                .reduce("", (left, right) -> left + '\n' + right)
                .toLowerCase(Locale.ROOT);
        long matched = terms.stream().filter(evidenceText::contains).count();
        return (double) matched / terms.size();
    }

    static Set<String> queryTerms(String question) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        Matcher asciiMatcher = ASCII_TERM_PATTERN.matcher(question);
        while (asciiMatcher.find()) {
            String term = asciiMatcher.group().toLowerCase(Locale.ROOT);
            if (!ENGLISH_STOP_TERMS.contains(term)) {
                terms.add(term);
            }
        }
        for (String hanRun : question.split("[^\\p{IsHan}]+")) {
            String normalized = QUESTION_WORDS.matcher(hanRun).replaceAll(" ");
            for (String term : normalized.trim().split("\\s+")) {
                if (term.length() >= 2) {
                    terms.add(term);
                }
            }
        }
        return Set.copyOf(terms);
    }
}
