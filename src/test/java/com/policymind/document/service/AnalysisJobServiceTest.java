package com.policymind.document.service;

import com.policymind.document.dto.AnalysisJobRequest;
import com.policymind.document.enums.AnalysisJobType;
import com.policymind.document.enums.WorkflowStatus;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AnalysisJobServiceTest {

    @Mock
    AnalysisJobRepository analysisJobRepository;

    @Mock
    AnalysisResultRepository analysisResultRepository;

    @Mock
    AgentRunRepository agentRunRepository;

    @Mock
    AgentStepRepository agentStepRepository;

    @Mock
    DocumentRepository documentRepository;

    @Mock
    DocumentService documentService;

    @InjectMocks
    AnalysisJobService analysisJobService;

    @Test
    public void createQuestionAnswerJob_success_persistsJobResultAndTrace() {
        Document document = new Document();
        document.setId(99L);
        document.setFileName("policy.pdf");

        when(documentRepository.findById(99L)).thenReturn(Optional.of(document));
        when(analysisJobRepository.save(any(AnalysisJob.class))).thenAnswer(invocation -> {
            AnalysisJob job = invocation.getArgument(0);
            if (job.getId() == null) {
                job.setId(501L);
            }
            return job;
        });
        when(agentRunRepository.save(any(AgentRun.class))).thenAnswer(invocation -> {
            AgentRun run = invocation.getArgument(0);
            if (run.getId() == null) {
                run.setId(601L);
            }
            return run;
        });
        when(documentService.askQuestion(99L, "What changed?", "openai", "openai")).thenReturn(Map.of(
                "documentId", 99L,
                "question", "What changed?",
                "retrievedChunkIds", List.of(11L, 12L),
                "retrievedLineRanges", List.of("Lines 1-2"),
                "structuredOutput", Map.of(
                        "summary", "Policy changes summary",
                        "confidence", "high",
                        "risk_score", 2
                )
        ));

        AnalysisJobRequest request = new AnalysisJobRequest();
        request.setQuestion("What changed?");
        request.setEmbeddingProvider("openai");
        request.setAnswerProvider("openai");

        Map<String, Object> response = analysisJobService.createQuestionAnswerJob(99L, request);

        assertEquals(501L, response.get("jobId"));
        assertEquals(WorkflowStatus.COMPLETED, response.get("status"));
        verify(analysisResultRepository).save(any(AnalysisResult.class));
        verify(agentStepRepository, org.mockito.Mockito.atLeast(3)).save(any());
    }

    @Test
    public void getJobStatus_returnsStoredJobSummary() {
        Document document = new Document();
        document.setId(88L);

        AnalysisJob job = new AnalysisJob();
        job.setId(777L);
        job.setDocument(document);
        job.setJobType(AnalysisJobType.QUESTION_ANSWER);
        job.setStatus(WorkflowStatus.COMPLETED);
        job.setRequestQuestion("How risky is this?");

        when(analysisJobRepository.findById(777L)).thenReturn(Optional.of(job));

        Map<String, Object> response = analysisJobService.getJobStatus(777L);

        assertEquals(777L, response.get("jobId"));
        assertEquals(88L, response.get("documentId"));
        assertEquals(WorkflowStatus.COMPLETED, response.get("status"));
        assertNotNull(response.get("resultUrl"));
    }

    @Test
    public void getJobResult_returnsPersistedStructuredResult() {
        Document document = new Document();
        document.setId(33L);

        AnalysisJob job = new AnalysisJob();
        job.setId(808L);
        job.setDocument(document);
        job.setJobType(AnalysisJobType.QUESTION_ANSWER);
        job.setStatus(WorkflowStatus.COMPLETED);

        AnalysisResult result = new AnalysisResult();
        result.setAnalysisJob(job);
        result.setDocument(document);
        result.setSummary("Policy summary");
        result.setConfidence("high");
        result.setRiskScore(1);
        result.setResultJson("{\"answer\":\"ok\"}");
        result.setCreatedAt(LocalDateTime.now());
        result.setFinalizedAt(LocalDateTime.now());

        when(analysisJobRepository.findById(808L)).thenReturn(Optional.of(job));
        when(analysisResultRepository.findByAnalysisJobId(808L)).thenReturn(Optional.of(result));

        Map<String, Object> response = analysisJobService.getJobResult(808L);

        assertEquals(808L, response.get("jobId"));
        assertEquals("Policy summary", response.get("summary"));
        assertEquals("high", response.get("confidence"));
        assertTrue(response.get("result") instanceof Map);
    }

    @Test
    public void getJobTrace_returnsWorkflowRunAndSteps() {
        Document document = new Document();
        document.setId(55L);

        AnalysisJob job = new AnalysisJob();
        job.setId(909L);
        job.setDocument(document);

        AgentRun run = new AgentRun();
        run.setId(1001L);
        run.setAnalysisJob(job);
        run.setWorkflowName("complex_qna_v1");
        run.setStatus(WorkflowStatus.COMPLETED);
        run.setConfidence("medium");
        run.setLatencyMs(250L);
        run.setProviderSummary("embedding=openai,answer=openai");

        AgentStep step = new AgentStep();
        step.setId(1L);
        step.setAgentRun(run);
        step.setStepName("retrieval");
        step.setStepType("retrieval");
        step.setStatus(WorkflowStatus.COMPLETED);
        step.setInputSummary("documentId=55");
        step.setOutputSummary("chunks=[1,2]");

        when(analysisJobRepository.findById(909L)).thenReturn(Optional.of(job));
        when(agentRunRepository.findByAnalysisJobId(909L)).thenReturn(Optional.of(run));
        when(agentStepRepository.findByAgentRunIdOrderByCreatedAtAsc(1001L)).thenReturn(List.of(step));

        Map<String, Object> response = analysisJobService.getJobTrace(909L);

        assertEquals(909L, response.get("jobId"));
        assertEquals("complex_qna_v1", response.get("workflowName"));
        assertTrue(response.get("steps") instanceof List);
    }
}
