package uk.gov.hmcts.reform.translate.data;

import lombok.Data;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Table(name = "dictionary")
@Entity
@Data
public class DictionaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dictionary_id_gen")
    @SequenceGenerator(name = "dictionary_id_gen", sequenceName = "dictionary_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "english_phrase")
    private String englishPhrase;

    @Column(name = "translation_phrase")
    private String translationPhrase;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "translation_version", referencedColumnName = "version")
    private TranslationUploadEntity translationUpload;
}
