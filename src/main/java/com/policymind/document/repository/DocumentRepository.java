
package com.policymind.document.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.policymind.document.model.Document;

public interface DocumentRepository extends JpaRepository<Document, Long> {}
