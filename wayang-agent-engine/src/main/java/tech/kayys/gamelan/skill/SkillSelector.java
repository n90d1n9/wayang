package tech.kayys.gamelan.skill;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Selects which skills are relevant to a user message.
 *
 * <h2>Scoring algorithm</h2>
 * A simple weighted keyword overlap:
 * <ul>
 *   <li>+10  — exact skill name appears in the input</li>
 *   <li>+3   — keyword (≥5 chars) from description matches in the input</li>
 *   <li>+1   — short keyword (4 chars) matches</li>
 *   <li>Bonus for input words that also appear in the skill name</li>
 * </ul>
 *
 * <p>This intentionally errs on the side of activating slightly more skills
 * rather than fewer — an irrelevant skill wastes a few hundred tokens,
 * while a missing relevant skill makes the model unaware of key context.
 *
 * <p>Future enhancement: replace keyword matching with cosine similarity
 * over Gollek embeddings when an embedding model is available.
 */
@ApplicationScoped
public class SkillSelector {

    private static final int MAX_SKILLS = 3;
    private static final int MIN_SCORE  = 1;

    public List<Skill> select(String userMessage, List<Skill> allSkills) {
        if (allSkills.isEmpty() || userMessage == null || userMessage.isBlank()) {
            return List.of();
        }
        String input = userMessage.toLowerCase();
        Set<String> inputWords = tokenize(input);

        record Scored(Skill skill, int score) {}

        return allSkills.stream()
                .map(s -> new Scored(s, score(input, inputWords, s)))
                .filter(s -> s.score() >= MIN_SCORE)
                .sorted(Comparator.comparingInt(Scored::score).reversed())
                .limit(MAX_SKILLS)
                .map(Scored::skill)
                .toList();
    }

    private int score(String input, Set<String> inputWords, Skill skill) {
        int score = 0;

        // Exact skill name match in input → big boost
        if (input.contains(skill.name().replace("-", " "))
                || input.contains(skill.name())) {
            score += 10;
        }

        // Keyword overlap: description words found in the input
        Set<String> descWords = tokenize(skill.description() + " " + skill.name());
        for (String word : descWords) {
            if (!inputWords.contains(word)) continue;
            score += word.length() >= 5 ? 3 : 1;
        }

        return score;
    }

    private Set<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase().split("[\\s,;.!?\\-_/]+"))
                .filter(w -> w.length() >= 4)
                .collect(Collectors.toSet());
    }
}
