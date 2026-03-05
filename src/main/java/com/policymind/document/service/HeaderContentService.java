package com.policymind.document.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class HeaderContentService {

    private static final Logger logger = LoggerFactory.getLogger(HeaderContentService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<>() {
    };

    private final StringRedisTemplate redisTemplate;

    @Value("${app.header-content.csv-path:resource_docs/contracts_policies_jokes.csv}")
    private String csvPath;

    @Value("${app.header-content.redis-key:policymind:header-content}")
    private String redisKey;

    private volatile List<String> inMemoryCache = List.of(
            "PolicyMind AI simplifies policies and contracts into clear risk insights."
    );

    public HeaderContentService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public List<String> getHeaderLines() {
        List<String> fromRedis = readFromRedis();
        if (!fromRedis.isEmpty()) {
            inMemoryCache = fromRedis;
            return fromRedis;
        }

        List<String> fromCsv = readFromCsv();
        if (!fromCsv.isEmpty()) {
            inMemoryCache = fromCsv;
            writeToRedis(fromCsv);
            return fromCsv;
        }

        return inMemoryCache;
    }

    private List<String> readFromCsv() {
        try {
            Path path = Paths.get(csvPath);
            if (!path.isAbsolute()) {
                path = Paths.get("").resolve(path).normalize();
            }

            if (!Files.exists(path)) {
                logger.warn("Header CSV file not found: {}", path);
                return List.of();
            }

            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            List<String> results = new ArrayList<>();

            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line == null || line.isBlank()) {
                    continue;
                }

                String[] fields = line.split(",", 2);
                if (fields.length < 2) {
                    continue;
                }

                String content = sanitizeCsvValue(fields[1]);
                if (!content.isBlank()) {
                    results.add(content);
                }
            }

            return results;
        } catch (Exception ex) {
            logger.error("Failed to read header CSV content", ex);
            return List.of();
        }
    }

    private static String sanitizeCsvValue(String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim();
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            value = value.substring(1, value.length() - 1);
        }
        return value.replace("\"\"", "\"").trim();
    }

    private List<String> readFromRedis() {
        if (redisTemplate == null) {
            return List.of();
        }

        try {
            String cachedJson = redisTemplate.opsForValue().get(redisKey);
            if (cachedJson == null || cachedJson.isBlank()) {
                return List.of();
            }
            return OBJECT_MAPPER.readValue(cachedJson, LIST_OF_STRINGS);
        } catch (Exception ex) {
            logger.warn("Redis read failed for header content. Falling back to file.", ex);
            return List.of();
        }
    }

    private void writeToRedis(List<String> contents) {
        if (redisTemplate == null || contents == null || contents.isEmpty()) {
            return;
        }

        try {
            redisTemplate.opsForValue().set(redisKey, OBJECT_MAPPER.writeValueAsString(contents));
        } catch (Exception ex) {
            logger.warn("Redis write failed for header content cache.", ex);
        }
    }
}
