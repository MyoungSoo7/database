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

import java.util.concurrent.locks.ReentrantLock;

@RestController
@RequestMapping("/api")
public class SqlExecuteController {

    /**
     * 모든 문제가 같은 스키마(sqltest)를 공유하고, 실행마다 schema.sql 로 테이블을 다시 만든다.
     * 두 요청이 겹치면 한쪽의 재생성·DROP 이 다른 쪽 채점 중간에 끼어든다 — 그래서 한 번에 하나씩.
     * 쿼리 타임아웃이 5초라 대기도 그 이상 길어지지 않는다 (replicaCount 1 전제).
     */
    private final ReentrantLock sandbox = new ReentrantLock(true);

    private final SqlExecuteService sqlExecuteService;
    private final SqlValidationService sqlValidationService;

    public SqlExecuteController(SqlExecuteService sqlExecuteService, SqlValidationService sqlValidationService) {
        this.sqlExecuteService = sqlExecuteService;
        this.sqlValidationService = sqlValidationService;
    }

    @PostMapping("/execute")
    public SqlResult execute(@RequestBody SqlRequest request) {
        sandbox.lock();
        try {
            return sqlExecuteService.execute(request.problemId(), request.sql());
        } finally {
            sandbox.unlock();
        }
    }

    @PostMapping("/submit")
    public SubmitResult submit(@RequestBody SqlRequest request) {
        sandbox.lock();
        try {
            return sqlValidationService.validate(request.problemId(), request.sql());
        } finally {
            sandbox.unlock();
        }
    }
}
