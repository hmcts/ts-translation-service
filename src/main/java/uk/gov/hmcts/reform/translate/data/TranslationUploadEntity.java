package uk.gov.hmcts.reform.translate.data;

import lombok.Data;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Table(name = "translation_upload")
@Entity
@Data
public class TranslationUploadEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "translation_version_gen")
    @SequenceGenerator(name = "translation_version_gen", sequenceName = "translation_version_seq", allocationSize = 1)
    @Column(name = "version")
    private Long version;

    @Column(name = "uploaded")
    private LocalDateTime uploaded;

    @Column(name = "user_id")
    private String userId;
}
