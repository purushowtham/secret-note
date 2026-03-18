package jar.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "notes")
public class Note {

    @Id
    @Column(nullable = false, unique = true)
    private String id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String encryptedContent;

    @Column(nullable = false)
    private String salt;

    @Column(nullable = false)
    private String iv;

    @Column(nullable = false)
    private String passwordHash;

    @Column(name = "secret_code")
    private String secretCode;

    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<FileAttachment> attachments = new java.util.ArrayList<>();

}
