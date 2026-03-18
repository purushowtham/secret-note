package jar.dto;

import jar.model.FileAttachment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NoteResponseDto {
    private String content;
    private List<FileAttachment> attachments;
}
