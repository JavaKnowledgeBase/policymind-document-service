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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DocumentControllerTest {

    @Test
    public void uploadEndpoint_returnsOk() throws Exception {
        DocumentService ds = Mockito.mock(DocumentService.class);
        when(ds.processDocument(any())).thenReturn(java.util.Map.of("message","ok"));

        DocumentController controller = new DocumentController(ds);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        MockMultipartFile file = new MockMultipartFile("file","test.pdf", MediaType.APPLICATION_PDF_VALUE, "hi".getBytes());

        mvc.perform(multipart("/upload").file(file))
                .andExpect(status().isOk());
    }
}
