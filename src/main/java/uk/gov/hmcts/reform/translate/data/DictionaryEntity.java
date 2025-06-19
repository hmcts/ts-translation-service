package uk.gov.hmcts.reform.translate.data;

import lombok.Data;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

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

    @Column(name = "yes_or_no")
    private Boolean yesOrNo;

    @Column(name = "yes")
    private String yes;

    @Column(name = "no")
    private String no;

    public boolean isYesOrNo() {
        return yesOrNo == null ? false : yesOrNo.booleanValue();
    }
}
