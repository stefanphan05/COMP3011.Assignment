package comp3011.assignment.client;

/** What one transcription produced*/
public record Transcription(String text, long inputTokens, long outputTokens) {}
