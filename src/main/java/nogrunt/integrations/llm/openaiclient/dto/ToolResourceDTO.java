package nogrunt.integrations.llm.openaiclient.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolResourceDTO(
        List<String> vectorStoreIds,
        List<String> fileIds) {}
