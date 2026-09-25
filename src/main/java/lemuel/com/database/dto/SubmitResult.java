package lemuel.com.database.dto;

/**
 * @param plan 튜닝 문제에서 채점에 쓴 EXPLAIN 결과 (그 외엔 null)
 */
public record SubmitResult(
    boolean correct,
    SqlResult userResult,
    SqlResult expectedResult,
    String feedback,
    SqlResult plan
) {
    public SubmitResult(boolean correct, SqlResult userResult, SqlResult expectedResult, String feedback) {
        this(correct, userResult, expectedResult, feedback, null);
    }
}
