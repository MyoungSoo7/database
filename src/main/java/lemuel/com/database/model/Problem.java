package lemuel.com.database.model;

public record Problem(
    String id,
    String title,
    String category,
    int difficulty,
    String description,
    String type,
    String schema,
    Expected expected,
    Validation validation,
    String hint
) {}
