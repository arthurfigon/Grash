package com.grash.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

/**
 * Banco de palavras do jogo do Impostor: um JSON embutido no jar
 * (`word-bank.json`), carregado uma única vez na subida do servidor pra um
 * {@code Map} em memória — sem banco de dados, sem I/O em disco durante o
 * jogo, sorteio de tema+palavra é O(1). Fácil de editar (só mexer no JSON)
 * e de estender com novos temas sem tocar em código.
 */
@Service
public class WordBankService {

    private static final Logger log = LoggerFactory.getLogger(WordBankService.class);
    private static final int MIN_WORDS_PER_THEME = 50;

    private final Map<String, List<String>> themes;
    private final List<String> themeNames;
    private final SecureRandom random = new SecureRandom();

    public WordBankService(ObjectMapper objectMapper) throws IOException {
        ClassPathResource resource = new ClassPathResource("word-bank.json");
        try (InputStream in = resource.getInputStream()) {
            this.themes = objectMapper.readValue(in, new TypeReference<Map<String, List<String>>>() {
            });
        }
        this.themeNames = List.copyOf(themes.keySet());

        themes.forEach((theme, words) -> {
            if (words.size() < MIN_WORDS_PER_THEME) {
                log.warn("Tema '{}' tem só {} palavras (mínimo recomendado: {})", theme, words.size(), MIN_WORDS_PER_THEME);
            }
        });
        log.info("Banco de palavras carregado: {} temas, {} palavras no total",
                themeNames.size(), themes.values().stream().mapToInt(List::size).sum());
    }

    public record ThemeWord(String theme, String word) {
    }

    public ThemeWord pickRandom() {
        String theme = themeNames.get(random.nextInt(themeNames.size()));
        return new ThemeWord(theme, pickRandomWordFromTheme(theme));
    }

    public List<String> listThemes() {
        return themeNames;
    }

    public boolean isKnownTheme(String theme) {
        return themes.containsKey(theme);
    }

    /** Se o tema não existir, sorteia de qualquer tema (defensivo — não deveria acontecer, o frontend só manda temas válidos). */
    public String pickRandomWordFromTheme(String theme) {
        List<String> words = themes.get(theme);
        if (words == null || words.isEmpty()) {
            return pickRandom().word();
        }
        return words.get(random.nextInt(words.size()));
    }
}
