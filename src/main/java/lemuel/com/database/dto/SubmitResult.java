package lemuel.com.database.dto;

public record SubmitResult(
    boolean correct,
    SqlResult userResult,
    SqlResult expectedResult,
    String feedback
) {}
