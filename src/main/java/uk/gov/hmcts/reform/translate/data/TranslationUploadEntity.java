package uk.gov.hmcts.reform.translate.data;

import lombok.Data;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

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
