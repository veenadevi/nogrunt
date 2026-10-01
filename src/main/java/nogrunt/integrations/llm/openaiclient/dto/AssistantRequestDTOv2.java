package nogrunt.integrations.llm.openaiclient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AssistantRequestDTOv2(
        String model,
        String instructions,
        double temperature,
//        @JsonProperty("top_p") Double topP,
        List<ToolDTO> tools) {

    public static record ToolDTO(
            String type) {}
}
