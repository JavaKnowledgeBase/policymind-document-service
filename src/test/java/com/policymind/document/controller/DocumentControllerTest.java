package com.policymind.document.controller;

import com.policymind.document.service.AnalysisJobService;
import com.policymind.document.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DocumentControllerTest {

    @Test
    public void uploadEndpoint_returnsAccepted() throws Exception {
        DocumentService ds = Mockito.mock(DocumentService.class);
        AnalysisJobService analysisJobService = Mockito.mock(AnalysisJobService.class);
        when(ds.submitDocument(any())).thenReturn(java.util.Map.of("message", "accepted"));

        DocumentController controller = new DocumentController(ds, analysisJobService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "hi".getBytes());

        mvc.perform(multipart("/upload").file(file))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", "0"))
                .andExpect(header().string("Vary", org.hamcrest.Matchers.containsString("Authorization")));
    }

    @Test
    public void documentStatusEndpoint_returnsOk() throws Exception {
        DocumentService ds = Mockito.mock(DocumentService.class);
        AnalysisJobService analysisJobService = Mockito.mock(AnalysisJobService.class);
        when(ds.getDocumentStatus(42L)).thenReturn(java.util.Map.of("documentId", 42L, "status", "PROCESSING"));

        DocumentController controller = new DocumentController(ds, analysisJobService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/documents/42"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", "0"));
    }

    @Test
    public void uploadEndpoint_returnsBadRequestWhenProcessingFails() throws Exception {
        DocumentService ds = Mockito.mock(DocumentService.class);
        AnalysisJobService analysisJobService = Mockito.mock(AnalysisJobService.class);
        when(ds.submitDocument(any())).thenThrow(new com.policymind.document.exception.DocumentProcessingException("Uploaded file is empty."));

        DocumentController controller = new DocumentController(ds, analysisJobService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[0]);

        mvc.perform(multipart("/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.error").value("Uploaded file is empty."));
    }

    @Test
    public void askEndpoint_returnsBadRequestAndNoStoreWhenQuestionMissing() throws Exception {
        DocumentService ds = Mockito.mock(DocumentService.class);
        AnalysisJobService analysisJobService = Mockito.mock(AnalysisJobService.class);

        DocumentController controller = new DocumentController(ds, analysisJobService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(post("/42/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.error").value("Question is required"));
    }

    @Test
    public void createAnalysisJobEndpoint_returnsAccepted() throws Exception {
        DocumentService ds = Mockito.mock(DocumentService.class);
        AnalysisJobService analysisJobService = Mockito.mock(AnalysisJobService.class);
        when(analysisJobService.createQuestionAnswerJob(org.mockito.ArgumentMatchers.eq(42L), any()))
                .thenReturn(java.util.Map.of("jobId", 900L, "status", "COMPLETED"));

        DocumentController controller = new DocumentController(ds, analysisJobService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(post("/documents/42/analysis-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What changed?\"}"))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.jobId").value(900L));
    }

    @Test
    public void getAnalysisJobStatusEndpoint_returnsOk() throws Exception {
        DocumentService ds = Mockito.mock(DocumentService.class);
        AnalysisJobService analysisJobService = Mockito.mock(AnalysisJobService.class);
        when(analysisJobService.getJobStatus(900L)).thenReturn(java.util.Map.of("jobId", 900L, "status", "COMPLETED"));

        DocumentController controller = new DocumentController(ds, analysisJobService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/analysis-jobs/900"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(900L));
    }

    @Test
    public void getAnalysisJobResultEndpoint_returnsOk() throws Exception {
        DocumentService ds = Mockito.mock(DocumentService.class);
        AnalysisJobService analysisJobService = Mockito.mock(AnalysisJobService.class);
        when(analysisJobService.getJobResult(900L)).thenReturn(java.util.Map.of("jobId", 900L, "summary", "Policy summary"));

        DocumentController controller = new DocumentController(ds, analysisJobService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/analysis-jobs/900/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Policy summary"));
    }

    @Test
    public void getAnalysisJobTraceEndpoint_returnsOk() throws Exception {
        DocumentService ds = Mockito.mock(DocumentService.class);
        AnalysisJobService analysisJobService = Mockito.mock(AnalysisJobService.class);
        when(analysisJobService.getJobTrace(900L)).thenReturn(java.util.Map.of("jobId", 900L, "workflowName", "complex_qna_v1"));

        DocumentController controller = new DocumentController(ds, analysisJobService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/analysis-jobs/900/trace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowName").value("complex_qna_v1"));
    }
}
