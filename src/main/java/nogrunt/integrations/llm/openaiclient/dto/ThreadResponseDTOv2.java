package nogrunt.integrations.llm.openaiclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
@JsonIgnoreProperties(ignoreUnknown = true)
public record ThreadResponseDTOv2(
        String id,
        String object,
        @JsonProperty("created_at")
        long createdAt,
        Map<String, Object> metadata,
        List<ToolDTO> tools,
        @JsonProperty("tool_resources")
        Map<String, ToolResourceDTO> toolResources) {

    // Inner class for ToolDTO
    public record ToolDTO(
            String type) {}

    // Inner class for ToolResourceDTO
    public record ToolResourceDTO(
            List<String> vectorStoreIds,
            List<String> fileIds) {}
}