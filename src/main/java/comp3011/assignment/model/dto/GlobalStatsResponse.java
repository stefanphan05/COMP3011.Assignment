package comp3011.assignment.model.dto;

// Using long not int since int only 32 bits and the document specifically says format: int64
public record GlobalStatsResponse(
    long inputTokens,
    long outputTokens
) {
}
