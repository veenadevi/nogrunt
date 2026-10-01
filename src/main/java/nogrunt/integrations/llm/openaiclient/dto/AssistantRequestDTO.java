package nogrunt.integrations.llm.openaiclient.dto;

public record AssistantRequestDTO(String model, String instructions, double temperature) {}
