package jar.controller;

import jar.dto.NoteResponseDto;
import jar.model.FileAttachment;
import jar.service.NoteService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/note")
@RequiredArgsConstructor
public class NoteApiController {

    private final NoteService noteService;

    @PostMapping("/access")
    public ResponseEntity<?> accessNote(@RequestBody AccessRequest request) {
        try {
            NoteResponseDto responseDto = noteService.accessNote(request.getCode());
            return ResponseEntity.ok(new SuccessResponse(responseDto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error"));
        }
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveNote(@RequestBody SaveRequest request) {
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Code cannot be empty"));
        }
        try {
            noteService.saveNote(request.getCode(), request.getContent());
            return ResponseEntity.ok(new SuccessResponse("Note saved successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error"));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteNote(@RequestBody AccessRequest request) {
        try {
            noteService.deleteNote(request.getCode());
            return ResponseEntity.ok(new SuccessResponse("Note deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error"));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("code") String code, @RequestParam("file") MultipartFile file) {
        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Code cannot be empty"));
        }
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("File is empty"));
        }
        try {
            FileAttachment attachment = noteService.uploadFile(code, file);
            return ResponseEntity.ok(new SuccessResponse(attachment));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error"));
        }
    }

    @DeleteMapping("/file")
    public ResponseEntity<?> deleteFile(@RequestBody DeleteFileRequest request) {
        if (request.getCode() == null || request.getCode().trim().isEmpty() || request.getFileId() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid request"));
        }
        try {
            noteService.deleteFile(request.getCode(), request.getFileId());
            return ResponseEntity.ok(new SuccessResponse("File deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error"));
        }
    }

    @Data
    public static class AccessRequest {
        private String code;
    }

    @Data
    public static class SaveRequest {
        private String code;
        private String content;
    }

    @Data
    public static class DeleteFileRequest {
        private String code;
        private Long fileId;
    }

    @Data
    public static class SuccessResponse {
        private final boolean success = true;
        private final Object data;
        
        public SuccessResponse(Object data) {
            this.data = data;
        }
    }

    @Data
    public static class ErrorResponse {
        private final boolean success = false;
        private final String error;
        
        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
