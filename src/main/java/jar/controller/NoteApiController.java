package jar.controller;

import jar.service.NoteService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/note")
@RequiredArgsConstructor
public class NoteApiController {

    private final NoteService noteService;

    @PostMapping("/access")
    public ResponseEntity<?> accessNote(@RequestBody AccessRequest request) {
        try {
            String content = noteService.accessNote(request.getCode());
            return ResponseEntity.ok(new SuccessResponse(content));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
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
    public static class SuccessResponse {
        private final boolean success = true;
        private final String data;
        
        public SuccessResponse(String data) {
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
