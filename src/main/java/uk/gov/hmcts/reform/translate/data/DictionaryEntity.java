package uk.gov.hmcts.reform.translate.data;

import lombok.Data;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Table(name = "dictionary")
@Entity
@Data
public class DictionaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY,
        generator = "dictionary_id_seq")
    @Column(name = "id")
    private Integer id;

    @Column(name = "english_phrase")
    private String englishPhrase;

    @Column(name = "translation_phrase")
    private String translationPhrase;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "translation_version", referencedColumnName = "version")
    private TranslationUploadEntity translationUpload;
}
