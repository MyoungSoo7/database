package lemuel.com.database.dto;

import java.util.List;

public record SqlResult(
    List<String> columns,
    List<List<Object>> rows,
    String message,
    long executionTime
) {}
