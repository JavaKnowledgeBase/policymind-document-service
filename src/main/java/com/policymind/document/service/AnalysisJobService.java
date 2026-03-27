package com.policymind.document.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymind.document.dto.AnalysisJobRequest;
import com.policymind.document.enums.AnalysisJobType;
import com.policymind.document.enums.WorkflowStatus;
import com.policymind.document.exception.DocumentProcessingException;
import com.policymind.document.model.AgentRun;
import com.policymind.document.model.AgentStep;
import com.policymind.document.model.AnalysisJob;
import com.policymind.document.model.AnalysisResult;
import com.policymind.document.model.Document;
import com.policymind.document.repository.AgentRunRepository;
import com.policymind.document.repository.AgentStepRepository;
import com.policymind.document.repository.AnalysisJobRepository;
import com.policymind.document.repository.AnalysisResultRepository;
import com.policymind.document.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalysisJobService {

    private final AnalysisJobRepository analysisJobRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final AgentRunRepository agentRunRepository;
    private final AgentStepRepository agentStepRepository;
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalysisJobService(AnalysisJobRepository analysisJobRepository,
                              AnalysisResultRepository analysisResultRepository,
                              AgentRunRepository agentRunRepository,
                              AgentStepRepository agentStepRepository,
                              DocumentRepository documentRepository,
                              DocumentService documentService) {
        this.analysisJobRepository = analysisJobRepository;
        this.analysisResultRepository = analysisResultRepository;
        this.agentRunRepository = agentRunRepository;
        this.agentStepRepository = agentStepRepository;
        this.documentRepository = documentRepository;
        this.documentService = documentService;
    }

    public Map<String, Object> createQuestionAnswerJob(Long documentId, AnalysisJobRequest request) {
        String question = request == null || request.getQuestion() == null ? null : request.getQuestion().trim();
        if (question == null || question.isBlank()) {
            throw new DocumentProcessingException("Question is required.");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentProcessingException("Document not found: " + documentId));

        LocalDateTime now = LocalDateTime.now();
        AnalysisJob job = new AnalysisJob();
        job.setDocument(document);
        job.setJobType(AnalysisJobType.QUESTION_ANSWER);
        job.setStatus(WorkflowStatus.PROCESSING);
        job.setCreatedAt(now);
        job.setStartedAt(now);
        job.setPriority("NORMAL");
        job.setEmbeddingProvider(request.getEmbeddingProvider());
        job.setAnswerProvider(request.getAnswerProvider());
        job.setRequestQuestion(question);
        job = analysisJobRepository.save(job);

        AgentRun run = new AgentRun();
        run.setAnalysisJob(job);
        run.setWorkflowName("complex_qna_v1");
        run.setProviderSummary(providerSummary(request));
        run.setStatus(WorkflowStatus.PROCESSING);
        run.setCreatedAt(now);
        run = agentRunRepository.save(run);

        createStep(run, "planner", "planner", WorkflowStatus.COMPLETED,
                "question='" + truncate(question, 160) + "'",
                "workflow=QUESTION_ANSWER", "workflow_selector", null, 0L);

        LocalDateTime executionStart = LocalDateTime.now();
        try {
            Map<String, Object> response = documentService.askQuestion(
                    documentId,
                    question,
                    request.getEmbeddingProvider(),
                    request.getAnswerProvider()
            );
            long totalLatencyMs = Duration.between(executionStart, LocalDateTime.now()).toMillis();

            if (response.containsKey("error")) {
                String error = String.valueOf(response.get("error"));
                createStep(run, "analysis", "analyst", WorkflowStatus.FAILED,
                        "documentId=" + documentId,
                        error, "document_qna", request.getAnswerProvider(), totalLatencyMs);

                run.setStatus(WorkflowStatus.FAILED);
                run.setFailureReason(error);
                run.setLatencyMs(totalLatencyMs);
                run.setCompletedAt(LocalDateTime.now());
                agentRunRepository.save(run);

                job.setStatus(WorkflowStatus.FAILED);
                job.setFailureReason(error);
                job.setCompletedAt(LocalDateTime.now());
                analysisJobRepository.save(job);
                return toJobStatusMap(job);
            }

            createStep(run, "retrieval", "retrieval", WorkflowStatus.COMPLETED,
                    "documentId=" + documentId,
                    "chunks=" + response.getOrDefault("retrievedChunkIds", List.of()),
                    "pgvector_search", request.getEmbeddingProvider(), Math.max(totalLatencyMs / 2, 1));

            Map<String, Object> structuredOutput = mapValue(response.get("structuredOutput"));
            String confidence = stringValue(structuredOutput.get("confidence"), "unknown");
            Integer riskScore = intValue(structuredOutput.get("risk_score"));
            String summary = stringValue(structuredOutput.get("summary"), "");

            createStep(run, "analysis", "analyst", WorkflowStatus.COMPLETED,
                    "question='" + truncate(question, 160) + "'",
                    "summary='" + truncate(summary, 200) + "'", "document_qna", request.getAnswerProvider(), totalLatencyMs);

            createStep(run, "verification", "verifier", WorkflowStatus.COMPLETED,
                    "confidence='" + confidence + "'",
                    "citations=" + response.getOrDefault("retrievedLineRanges", List.of()),
                    "response_validation", null, Math.max(totalLatencyMs / 4, 1));

            AnalysisResult result = new AnalysisResult();
            result.setAnalysisJob(job);
            result.setDocument(document);
            result.setSummary(summary);
            result.setConfidence(confidence);
            result.setRiskScore(riskScore);
            result.setResultJson(writeJson(response));
            result.setCreatedAt(LocalDateTime.now());
            result.setFinalizedAt(LocalDateTime.now());
            analysisResultRepository.save(result);

            run.setStatus(WorkflowStatus.COMPLETED);
            run.setConfidence(confidence);
            run.setLatencyMs(totalLatencyMs);
            run.setCompletedAt(LocalDateTime.now());
            agentRunRepository.save(run);

            job.setStatus(WorkflowStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            analysisJobRepository.save(job);
            return toJobStatusMap(job);
        } catch (Exception ex) {
            long totalLatencyMs = Duration.between(executionStart, LocalDateTime.now()).toMillis();
            String error = rootCauseMessage(ex);

            createStep(run, "analysis", "analyst", WorkflowStatus.FAILED,
                    "question='" + truncate(question, 160) + "'",
                    error, "document_qna", request.getAnswerProvider(), totalLatencyMs);

            run.setStatus(WorkflowStatus.FAILED);
            run.setFailureReason(error);
            run.setLatencyMs(totalLatencyMs);
            run.setCompletedAt(LocalDateTime.now());
            agentRunRepository.save(run);

            job.setStatus(WorkflowStatus.FAILED);
            job.setFailureReason(error);
            job.setCompletedAt(LocalDateTime.now());
            analysisJobRepository.save(job);
            return toJobStatusMap(job);
        }
    }

    public Map<String, Object> getJobStatus(Long jobId) {
        AnalysisJob job = analysisJobRepository.findById(jobId)
                .orElseThrow(() -> new DocumentProcessingException("Analysis job not found: " + jobId));
        return toJobStatusMap(job);
    }

    public Map<String, Object> getJobResult(Long jobId) {
        AnalysisJob job = analysisJobRepository.findById(jobId)
                .orElseThrow(() -> new DocumentProcessingException("Analysis job not found: " + jobId));
        AnalysisResult result = analysisResultRepository.findByAnalysisJobId(jobId)
                .orElseThrow(() -> new DocumentProcessingException("Analysis result not ready for job: " + jobId));

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", job.getId());
        response.put("documentId", job.getDocument() == null ? null : job.getDocument().getId());
        response.put("jobType", job.getJobType());
        response.put("status", job.getStatus());
        response.put("summary", result.getSummary());
        response.put("confidence", result.getConfidence());
        response.put("riskScore", result.getRiskScore());
        response.put("result", readJson(result.getResultJson()));
        response.put("createdAt", result.getCreatedAt());
        response.put("finalizedAt", result.getFinalizedAt());
        return response;
    }

    public Map<String, Object> getJobTrace(Long jobId) {
        AnalysisJob job = analysisJobRepository.findById(jobId)
                .orElseThrow(() -> new DocumentProcessingException("Analysis job not found: " + jobId));
        AgentRun run = agentRunRepository.findByAnalysisJobId(jobId)
                .orElseThrow(() -> new DocumentProcessingException("Agent trace not found for job: " + jobId));
        List<AgentStep> steps = agentStepRepository.findByAgentRunIdOrderByCreatedAtAsc(run.getId());

        List<Map<String, Object>> traceSteps = steps.stream().map(step -> {
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", step.getId());
            entry.put("stepName", step.getStepName());
            entry.put("stepType", step.getStepType());
            entry.put("status", step.getStatus());
            entry.put("inputSummary", step.getInputSummary());
            entry.put("outputSummary", step.getOutputSummary());
            entry.put("toolName", step.getToolName());
            entry.put("modelName", step.getModelName());
            entry.put("latencyMs", step.getLatencyMs());
            entry.put("createdAt", step.getCreatedAt());
            entry.put("completedAt", step.getCompletedAt());
            return entry;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", job.getId());
        response.put("documentId", job.getDocument() == null ? null : job.getDocument().getId());
        response.put("workflowName", run.getWorkflowName());
        response.put("status", run.getStatus());
        response.put("confidence", run.getConfidence());
        response.put("latencyMs", run.getLatencyMs());
        response.put("providerSummary", run.getProviderSummary());
        response.put("steps", traceSteps);
        return response;
    }

    private Map<String, Object> toJobStatusMap(AnalysisJob job) {
        Map<String, Object> response = new HashMap<>();
        response.put("jobId", job.getId());
        response.put("documentId", job.getDocument() == null ? null : job.getDocument().getId());
        response.put("jobType", job.getJobType());
        response.put("status", job.getStatus());
        response.put("question", job.getRequestQuestion());
        response.put("embeddingProvider", job.getEmbeddingProvider());
        response.put("answerProvider", job.getAnswerProvider());
        response.put("failureReason", job.getFailureReason());
        response.put("createdAt", job.getCreatedAt());
        response.put("startedAt", job.getStartedAt());
        response.put("completedAt", job.getCompletedAt());
        response.put("resultUrl", "/analysis-jobs/" + job.getId() + "/result");
        response.put("traceUrl", "/analysis-jobs/" + job.getId() + "/trace");
        return response;
    }

    private void createStep(AgentRun run,
                            String stepName,
                            String stepType,
                            WorkflowStatus status,
                            String inputSummary,
                            String outputSummary,
                            String toolName,
                            String modelName,
                            Long latencyMs) {
        AgentStep step = new AgentStep();
        step.setAgentRun(run);
        step.setStepName(stepName);
        step.setStepType(stepType);
        step.setStatus(status);
        step.setInputSummary(inputSummary);
        step.setOutputSummary(outputSummary);
        step.setToolName(toolName);
        step.setModelName(modelName);
        step.setLatencyMs(latencyMs);
        step.setCreatedAt(LocalDateTime.now());
        step.setCompletedAt(LocalDateTime.now());
        agentStepRepository.save(step);
    }

    private String providerSummary(AnalysisJobRequest request) {
        return "embedding=" + stringValue(request == null ? null : request.getEmbeddingProvider(), "default")
                + ",answer=" + stringValue(request == null ? null : request.getAnswerProvider(), "default");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isBlank() ? fallback : normalized;
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new DocumentProcessingException("Failed to serialize analysis result.", ex);
        }
    }

    private Object readJson(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception ex) {
            return json;
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
