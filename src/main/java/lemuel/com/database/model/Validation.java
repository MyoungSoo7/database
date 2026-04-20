package lemuel.com.database.model;

import java.util.Map;

public record Validation(
    String type,
    Map<String, String> checkPerProfile,
    int expectedValue
) {}
