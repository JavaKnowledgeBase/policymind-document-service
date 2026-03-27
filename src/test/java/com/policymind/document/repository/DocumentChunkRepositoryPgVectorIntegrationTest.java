package com.policymind.document.repository;

import com.policymind.document.entity.DocumentChunk;
import com.policymind.document.model.Document;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.sql.init.mode=always",
        "spring.jpa.defer-datasource-initialization=true",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class DocumentChunkRepositoryPgVectorIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg15").asCompatibleSubstituteFor("postgres")
    )
            .withDatabaseName("policymind_test")
            .withUsername("policymind")
            .withPassword("policymind");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    DocumentChunkRepository chunkRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    @Test
    void pgvectorSchemaInitializesAndSimilarityQueryReturnsNearestChunks() {
        Integer vectorExtension = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_extension WHERE extname = 'vector'",
                Integer.class
        );
        Integer vectorColumn = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'document_chunk' AND column_name = 'embedding_vector'",
                Integer.class
        );
        Integer vectorIndex = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'document_chunk' AND indexname = 'idx_document_chunk_embedding_vector_hnsw'",
                Integer.class
        );

        assertEquals(1, vectorExtension);
        assertEquals(1, vectorColumn);
        assertEquals(1, vectorIndex);

        Document document = new Document();
        document.setFileName("policy.pdf");
        document.setStatus("COMPLETED");
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        document = documentRepository.saveAndFlush(document);

        DocumentChunk bestMatch = new DocumentChunk();
        bestMatch.setDocument(document);
        bestMatch.setContent("Best matching clause");
        bestMatch.setEmbedding("[1.0,0.0,0.0]");
        bestMatch.setStartLine(1);
        bestMatch.setEndLine(2);
        bestMatch = chunkRepository.saveAndFlush(bestMatch);

        DocumentChunk secondMatch = new DocumentChunk();
        secondMatch.setDocument(document);
        secondMatch.setContent("Second matching clause");
        secondMatch.setEmbedding("[0.0,1.0,0.0]");
        secondMatch.setStartLine(3);
        secondMatch.setEndLine(4);
        secondMatch = chunkRepository.saveAndFlush(secondMatch);

        chunkRepository.updateEmbeddingVector(bestMatch.getId(), "[1.0,0.0,0.0]");
        chunkRepository.updateEmbeddingVector(secondMatch.getId(), "[0.0,1.0,0.0]");
        entityManager.flush();
        entityManager.clear();

        List<DocumentChunk> matches = chunkRepository.findTopSimilarChunks(document.getId(), "[1.0,0.0,0.0]", 2);

        assertEquals(2, matches.size());
        assertEquals(bestMatch.getId(), matches.get(0).getId());
        assertEquals(secondMatch.getId(), matches.get(1).getId());

        String storedVector = jdbcTemplate.queryForObject(
                "SELECT embedding_vector::text FROM document_chunk WHERE id = ?",
                String.class,
                bestMatch.getId()
        );
        assertFalse(storedVector == null || storedVector.isBlank());
        assertTrue(storedVector.contains("1"));
    }
}
