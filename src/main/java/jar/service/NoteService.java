package jar.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jar.dto.NoteResponseDto;
import jar.model.FileAttachment;
import jar.model.Note;
import jar.repository.FileAttachmentRepository;
import jar.repository.NoteRepository;
import jar.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class NoteService {

    private final NoteRepository noteRepository;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final CryptoUtil cryptoUtil;
    private final Cloudinary cloudinary;

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
            note.setSecretCode(code);
        }

        return noteRepository.save(note);
    }

    public NoteResponseDto accessNote(String code) throws Exception {
        String id = cryptoUtil.hashStringSHA256(code);
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Note not found"));

        if (!cryptoUtil.verifyPassword(code, note.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect code");
        }
        
        SecretKey secretKey = cryptoUtil.getSecretKey(code, note.getSalt());
        String content = cryptoUtil.decrypt(note.getEncryptedContent(), secretKey, note.getIv());
        
        return new NoteResponseDto(content, note.getAttachments());
    }

    public void deleteNote(String code) throws Exception {
        String id = cryptoUtil.hashStringSHA256(code);
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Note not found"));

        if (!cryptoUtil.verifyPassword(code, note.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect code");
        }
        
        for (FileAttachment attachment : note.getAttachments()) {
            cloudinary.uploader().destroy(attachment.getPublicId(), ObjectUtils.emptyMap());
        }
        
        noteRepository.delete(note);
    }

    public FileAttachment uploadFile(String code, MultipartFile file) throws Exception {
        String id = cryptoUtil.hashStringSHA256(code);
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Note not found"));

        if (!cryptoUtil.verifyPassword(code, note.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect code");
        }

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("resource_type", "auto"));
        String publicId = uploadResult.get("public_id").toString();
        String url = uploadResult.get("url").toString();

        FileAttachment attachment = new FileAttachment();
        attachment.setPublicId(publicId);
        attachment.setUrl(url);
        attachment.setOriginalFilename(file.getOriginalFilename());
        attachment.setNote(note);

        return fileAttachmentRepository.save(attachment);
    }

    public void deleteFile(String code, Long fileId) throws Exception {
        String id = cryptoUtil.hashStringSHA256(code);
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Note not found"));

        if (!cryptoUtil.verifyPassword(code, note.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect code");
        }

        FileAttachment attachment = fileAttachmentRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        if (!attachment.getNote().getId().equals(note.getId())) {
            throw new IllegalArgumentException("File does not belong to this note");
        }

        cloudinary.uploader().destroy(attachment.getPublicId(), ObjectUtils.emptyMap());
        fileAttachmentRepository.delete(attachment);
    }
}
