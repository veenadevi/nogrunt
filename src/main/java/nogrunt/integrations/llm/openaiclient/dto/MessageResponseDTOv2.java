package nogrunt.integrations.llm.openaiclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageResponseDTOv2(
        String id,
        String object,
        @JsonProperty("created_at")
        long createdAt,
        @JsonProperty("thread_id")
        String threadId,
        String role,
        List<Content> content,
        @JsonProperty("assistant_id")
        String assistantId,
        @JsonProperty("run_id")
        String runId,
        Map<String, Object> metadata,
        List<Attachment> attachments) {

    public static record Content(
            String type,
            Text text) {

        public static record Text(
                String value,
                List<Object> annotations) {}
    }

    public static record Attachment(
            @JsonProperty("file_id") String fileId,
            List<ToolDTO> tools) {}

    public static record ToolDTO(
            String type) {}
}

