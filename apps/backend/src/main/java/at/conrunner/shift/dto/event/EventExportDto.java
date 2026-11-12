package at.conrunner.shift.dto.event;

import java.io.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.MediaType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventExportDto {
    private InputStream exportStream;
    private String fileName;
    private MediaType mediaType;
}
