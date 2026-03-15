package com.policymind.document.controller;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class DocumentControllerTest {

    @Test
    public void uploadEndpoint_returnsAccepted() throws Exception {
        DocumentService ds = Mockito.mock(DocumentService.class);
        when(ds.submitDocument(any())).thenReturn(java.util.Map.of("message","accepted"));

        DocumentController controller = new DocumentController(ds);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        MockMultipartFile file = new MockMultipartFile("file","test.pdf", MediaType.APPLICATION_PDF_VALUE, "hi".getBytes());

        mvc.perform(multipart("/upload").file(file))
                .andExpect(status().isAccepted());
    }

    @Test
    public void documentStatusEndpoint_returnsOk() throws Exception {
        DocumentService ds = Mockito.mock(DocumentService.class);
        when(ds.getDocumentStatus(42L)).thenReturn(java.util.Map.of("documentId", 42L, "status", "PROCESSING"));

        DocumentController controller = new DocumentController(ds);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/documents/42"))
                .andExpect(status().isOk());
    }

    @Test
    public void uploadEndpoint_returnsBadRequestWhenProcessingFails() throws Exception {
        DocumentService ds = Mockito.mock(DocumentService.class);
        when(ds.submitDocument(any())).thenThrow(new com.policymind.document.exception.DocumentProcessingException("Uploaded file is empty."));

        DocumentController controller = new DocumentController(ds);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[0]);

        mvc.perform(multipart("/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Uploaded file is empty."));
    }
}
