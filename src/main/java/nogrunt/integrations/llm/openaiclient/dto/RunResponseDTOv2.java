package nogrunt.integrations.llm.openaiclient.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
@JsonIgnoreProperties(ignoreUnknown = true)
public record RunResponseDTOv2(
        String id,
        String object,
        @JsonProperty("created_at")
        long createdAt,
        @JsonProperty("assistant_id")
        String assistantId,
        @JsonProperty("thread_id")
        String threadId,
        String top_p,
        String status,
        @JsonProperty("started_at")
        Long startedAt, // Using Long to handle null values
        @JsonProperty("expires_at")
        Long expiresAt,
        @JsonProperty("cancelled_at")
        Long cancelledAt,
        @JsonProperty("failed_at")
        Long failedAt,
        @JsonProperty("completed_at")
        Long completedAt,
        @JsonProperty("last_error")
        String lastError,
        @JsonProperty("max_completion_tokens")
        String max_completion_tokens,
        @JsonProperty("max_prompt_tokens")
        String max_prompt_tokens,
        @JsonProperty("truncation_strategy")
        Map<String, Object> truncation_strategy,
        @JsonProperty("incomplete_details")
        Map<String, Object> incomplete_details,
        @JsonProperty("response_format")
        String response_format,
        @JsonProperty("tool_choice")
        String tool_choice,
        String model,
        String instructions,
        List<ToolDTO> tools,
        String required_action,
        Long temperature,
        Map<String, Object> usage,
        @JsonProperty("file_ids")
        List<String> fileIds,
        boolean parallel_tool_calls,
        Map<String, Object> metadata) {}
