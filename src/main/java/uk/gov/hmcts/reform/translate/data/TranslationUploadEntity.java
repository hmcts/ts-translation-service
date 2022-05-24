package uk.gov.hmcts.reform.translate.data;

import lombok.Data;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;


@Table(name = "translation_upload")
@Entity
@Data
public class TranslationUploadEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY,
        generator = "translation_version_seq")
    @Column(name = "version")
    private Integer version;

    @Column(name = "uploaded")
    private LocalDateTime uploaded;

    @Column(name = "user_id")
    private String userId;
}
