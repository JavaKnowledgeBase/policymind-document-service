package com.policymind.document.service;

import com.policymind.document.entity.DocumentChunk;
import com.policymind.document.model.Document;
import com.policymind.document.repository.DocumentChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ChunkService {

    private static final Logger logger = LoggerFactory.getLogger(ChunkService.class);
    private static final int CHUNK_SIZE = 800;
    private static final int MAX_CHUNK_LENGTH = 3000;
    private static final Pattern NUMBERED_HEADING = Pattern.compile("^(?:section\\s+)?\\d+(?:\\.\\d+)*[.)-]?\\s+.+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern BULLET_LINE = Pattern.compile("^(?:[-*]|\\d+[.)]|[a-zA-Z][.)])\\s+.+$");

    private final DocumentChunkRepository chunkRepository;

    public ChunkService(DocumentChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    public List<String> chunkText(String text) {
        return extractClauseChunks(text, null).stream().map(ClauseChunk::content).toList();
    }

    public List<ClauseChunk> extractClauseChunks(String text, String sourceName) {
        List<ClauseChunk> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        String currentSection = "General";
        StringBuilder currentClause = new StringBuilder();
        int clauseStartLine = -1;

        for (int i = 0; i < lines.length; i++) {
            String trimmedLine = lines[i] == null ? "" : lines[i].trim();
            int lineNumber = i + 1;

            if (trimmedLine.isBlank()) {
                if (currentClause.length() > 0) {
                    addClauseChunk(chunks, currentClause.toString(), currentSection, clauseStartLine, lineNumber - 1, sourceName);
                    currentClause.setLength(0);
                    clauseStartLine = -1;
                }
                continue;
            }

            if (isSectionHeading(trimmedLine)) {
                if (currentClause.length() > 0) {
                    addClauseChunk(chunks, currentClause.toString(), currentSection, clauseStartLine, lineNumber - 1, sourceName);
                    currentClause.setLength(0);
                    clauseStartLine = -1;
                }
                currentSection = sanitizeHeading(trimmedLine);
                continue;
            }

            if (startsNewClause(trimmedLine, currentClause)) {
                if (currentClause.length() > 0) {
                    addClauseChunk(chunks, currentClause.toString(), currentSection, clauseStartLine, lineNumber - 1, sourceName);
                    currentClause.setLength(0);
                }
                clauseStartLine = lineNumber;
            } else if (clauseStartLine < 0) {
                clauseStartLine = lineNumber;
            }

            if (currentClause.length() > 0) {
                currentClause.append('\n');
            }
            currentClause.append(trimmedLine);
        }

        if (currentClause.length() > 0) {
            addClauseChunk(chunks, currentClause.toString(), currentSection, clauseStartLine, lines.length, sourceName);
        }

        if (chunks.isEmpty()) {
            chunks.addAll(fallbackFixedChunks(text, sourceName));
        }

        logger.info("Structured clause extraction completed, source='{}', clauseCount={}", sourceName, chunks.size());
        return chunks;
    }

    public int getChunkSize() {
        return CHUNK_SIZE;
    }

    public void saveChunks(Document savedDoc, List<String> chunks) {
        for (String chunkText : chunks) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocument(savedDoc);
            chunk.setContent(trimContent(chunkText));
            chunk.setEmbedding(null);
            chunk.setChunkKind("legacy_text_chunk");
            chunk.setSectionTitle("General");
            chunk.setClauseType("general");
            chunk.setDomain("hr_internal_policy");
            chunk.setPolicyType(inferPolicyType(chunkText));
            chunk.setJurisdiction("United States");
            chunk.setSourceName(savedDoc == null ? null : savedDoc.getFileName());
            chunk.setRiskTags(joinTags(inferRiskTags(chunkText)));
            chunk.setReferenceClause(Boolean.FALSE);
            chunkRepository.save(chunk);
        }
    }

    private void addClauseChunk(List<ClauseChunk> chunks, String rawContent, String sectionTitle, int startLine, int endLine, String sourceName) {
        String content = trimContent(rawContent);
        if (content.isBlank()) {
            return;
        }
        String policyType = inferPolicyType(content + "\n" + sectionTitle + "\n" + sourceName);
        chunks.add(new ClauseChunk(content, Math.max(startLine, 1), Math.max(Math.max(startLine, 1), endLine), "policy_clause", sectionTitle,
                inferClauseType(content, sectionTitle), "hr_internal_policy", policyType, inferJurisdiction(content), sourceName,
                joinTags(inferRiskTags(content, sectionTitle)), false));
    }

    private List<ClauseChunk> fallbackFixedChunks(String text, String sourceName) {
        List<ClauseChunk> chunks = new ArrayList<>();
        String normalized = text == null ? "" : text.trim();
        for (int i = 0; i < normalized.length(); i += CHUNK_SIZE) {
            String part = trimContent(normalized.substring(i, Math.min(normalized.length(), i + CHUNK_SIZE)));
            if (!part.isBlank()) {
                chunks.add(new ClauseChunk(part, 1, 1, "fallback_text_chunk", "General", "general", "hr_internal_policy", inferPolicyType(part), inferJurisdiction(part), sourceName, joinTags(inferRiskTags(part)), false));
            }
        }
        return chunks;
    }

    private boolean isSectionHeading(String line) {
        if (line == null || line.isBlank() || line.length() > 90) {
            return false;
        }
        if (NUMBERED_HEADING.matcher(line).matches() || line.endsWith(":")) {
            return true;
        }
        if (line.equals(line.toUpperCase(Locale.ROOT)) && line.chars().anyMatch(Character::isLetter)) {
            return true;
        }
        String[] words = line.split("\\s+");
        if (words.length <= 4 && line.chars().noneMatch(Character::isDigit)) {
            boolean looksLikeTitle = true;
            for (String word : words) {
                if (word.isBlank()) {
                    continue;
                }
                char first = word.charAt(0);
                if (!Character.isUpperCase(first)) {
                    looksLikeTitle = false;
                    break;
                }
            }
            return looksLikeTitle;
        }
        return false;
    }

    private boolean startsNewClause(String line, StringBuilder currentClause) {
        if (line == null || line.isBlank()) {
            return false;
        }
        if (currentClause.length() == 0) {
            return true;
        }
        return BULLET_LINE.matcher(line).matches();
    }

    private String sanitizeHeading(String heading) {
        String cleaned = heading == null ? "General" : heading.trim();
        cleaned = cleaned.endsWith(":") ? cleaned.substring(0, cleaned.length() - 1).trim() : cleaned;
        return cleaned.isBlank() ? "General" : cleaned;
    }

    private String trimContent(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() <= MAX_CHUNK_LENGTH ? trimmed : trimmed.substring(0, MAX_CHUNK_LENGTH);
    }

    private String inferPolicyType(String text) {
        String normalized = normalize(text);
        if (normalized.contains("remote work") || normalized.contains("remote-work") || normalized.contains("work from home")) return "Remote Work Policy";
        if (normalized.contains("leave") || normalized.contains("pto") || normalized.contains("vacation")) return "Leave Policy";
        if (normalized.contains("attendance") || normalized.contains("timekeeping")) return "Attendance Policy";
        if (normalized.contains("harassment") || normalized.contains("discrimination") || normalized.contains("equal employment")) return "Workplace Conduct Policy";
        if (normalized.contains("employee") || normalized.contains("manager") || normalized.contains("employment")) return "HR Policy";
        return "Internal Company Policy";
    }

    private String inferClauseType(String content, String sectionTitle) {
        String normalized = normalize(sectionTitle + " " + content);
        if (normalized.contains("purpose") || normalized.contains("objective")) return "purpose";
        if (normalized.contains("scope") || normalized.contains("applies to")) return "scope";
        if (normalized.contains("eligibility") || normalized.contains("qualified")) return "eligibility";
        if (normalized.contains("responsibility") || normalized.contains("manager") || normalized.contains("employee must")) return "responsibility";
        if (normalized.contains("approval") || normalized.contains("must be approved")) return "approval";
        if (normalized.contains("exception") || normalized.contains("violation") || normalized.contains("disciplinary")) return "enforcement";
        return "general";
    }

    private String inferJurisdiction(String text) {
        String normalized = normalize(text);
        if (normalized.contains("united states") || normalized.contains("u.s.") || normalized.contains("federal") || normalized.contains("state law")) {
            return "United States";
        }
        return "General";
    }

    private Set<String> inferRiskTags(String... inputs) {
        String normalized = normalize(String.join(" ", inputs));
        Set<String> tags = new LinkedHashSet<>();
        if (normalized.contains("must") || normalized.contains("required") || normalized.contains("shall")) tags.add("mandatory_language");
        if (normalized.contains("manager") || normalized.contains("approval") || normalized.contains("supervisor")) tags.add("approval_dependency");
        if (normalized.contains("disciplinary") || normalized.contains("violation") || normalized.contains("termination")) tags.add("enforcement_risk");
        if (normalized.contains("confidential") || normalized.contains("privacy") || normalized.contains("personal data")) tags.add("privacy_risk");
        if (normalized.contains("leave") || normalized.contains("accommodation") || normalized.contains("equal employment")) tags.add("employment_compliance");
        if (tags.isEmpty()) tags.add("general_policy_risk");
        return tags;
    }

    private String joinTags(Set<String> tags) {
        return String.join(",", tags);
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }

    public record ClauseChunk(String content, int startLine, int endLine, String chunkKind, String sectionTitle, String clauseType,
                              String domain, String policyType, String jurisdiction, String sourceName, String riskTags, boolean referenceClause) {
    }
}



