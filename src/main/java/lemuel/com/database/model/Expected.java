package lemuel.com.database.model;

import java.util.List;

public record Expected(
    List<String> columns,
    List<List<Object>> rows,
    boolean orderMatters
) {}
