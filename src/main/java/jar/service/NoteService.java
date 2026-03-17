package jar.service;

import jar.model.Note;
import jar.repository.NoteRepository;
import jar.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final CryptoUtil cryptoUtil;

    public Note saveNote(String code, String content) throws Exception {
        String id = cryptoUtil.hashStringSHA256(code);
        Optional<Note> optionalNote = noteRepository.findById(id);

        Note note;
        if (optionalNote.isPresent()) {
            note = optionalNote.get();
            if (!cryptoUtil.verifyPassword(code, note.getPasswordHash())) {
                throw new IllegalArgumentException("Incorrect code/password for existing note");
            }
            String newIv = cryptoUtil.generateIv();
            SecretKey secretKey = cryptoUtil.getSecretKey(code, note.getSalt());
            String newEncryptedContent = cryptoUtil.encrypt(content, secretKey, newIv);
            note.setIv(newIv);
            note.setEncryptedContent(newEncryptedContent);
        } else {
            note = new Note();
            note.setId(id);
            String salt = cryptoUtil.generateSalt();
            String iv = cryptoUtil.generateIv();
            String passwordHash = cryptoUtil.hashPassword(code);
            SecretKey secretKey = cryptoUtil.getSecretKey(code, salt);
            String encryptedContent = cryptoUtil.encrypt(content, secretKey, iv);
            
            note.setEncryptedContent(encryptedContent);
            note.setSalt(salt);
            note.setIv(iv);
            note.setPasswordHash(passwordHash);
        }

        return noteRepository.save(note);
    }

    public String accessNote(String code) throws Exception {
        String id = cryptoUtil.hashStringSHA256(code);
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Note not found"));

        if (!cryptoUtil.verifyPassword(code, note.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect code");
        }
        
        SecretKey secretKey = cryptoUtil.getSecretKey(code, note.getSalt());
        return cryptoUtil.decrypt(note.getEncryptedContent(), secretKey, note.getIv());
    }

    public void deleteNote(String code) throws Exception {
        String id = cryptoUtil.hashStringSHA256(code);
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Note not found"));

        if (!cryptoUtil.verifyPassword(code, note.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect code");
        }
        
        noteRepository.delete(note);
    }
}
