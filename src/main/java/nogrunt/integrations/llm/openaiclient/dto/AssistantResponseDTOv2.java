package nogrunt.integrations.llm.openaiclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AssistantResponseDTOv2(
        String id,
        String object,
        @JsonProperty("created_at")
        long createdAt,
        String name,
        String description,
        String model,
        String instructions,
        List<ToolDTO> tools,
        @JsonProperty("tool_resources")
        Map<String, ToolResourceDTO> toolResources,
        Map<String, Object> metadata,
        @JsonProperty("top_p") // Add this line for the top_p field
        Double topP,
        @JsonProperty("temperature") // Added for temperature field
        Double temperature,
        @JsonProperty("file_ids") // Added for file_ids field
        List<String> fileIds,
        @JsonProperty("response_format") // Added for response_format field
        String responseFormat) {}



// Tool Resource DTO to represent resources for each tool
