package lemuel.com.database.controller;

import lemuel.com.database.dto.SqlRequest;
import lemuel.com.database.dto.SqlResult;
import lemuel.com.database.dto.SubmitResult;
import lemuel.com.database.service.SqlExecuteService;
import lemuel.com.database.service.SqlValidationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SqlExecuteController {

    private final SqlExecuteService sqlExecuteService;
    private final SqlValidationService sqlValidationService;

    public SqlExecuteController(SqlExecuteService sqlExecuteService, SqlValidationService sqlValidationService) {
        this.sqlExecuteService = sqlExecuteService;
        this.sqlValidationService = sqlValidationService;
    }

    @PostMapping("/execute")
    public SqlResult execute(@RequestBody SqlRequest request) {
        return sqlExecuteService.execute(request.problemId(), request.sql());
    }

    @PostMapping("/submit")
    public SubmitResult submit(@RequestBody SqlRequest request) {
        return sqlValidationService.validate(request.problemId(), request.sql());
    }
}
