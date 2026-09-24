package lemuel.com.database.controller;

import lemuel.com.database.dto.SqlRequest;
import lemuel.com.database.dto.SubmitResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 모든 문제가 한 스키마를 공유하고 실행마다 schema.sql 로 테이블을 다시 만든다.
 * 동시에 들어온 정답 제출이 서로의 DROP/CREATE 에 끼어 오답이 되면 안 된다.
 * 단발 래치로는 레이스가 안 잡혀서 배리어 + 여러 라운드로 겹치게 한다.
 */
@SpringBootTest
class SqlSandboxConcurrencyTest {

    private static final int THREADS = 8;
    private static final int ROUNDS = 15;

    @Autowired
    SqlExecuteController controller;

    @Test
    void concurrentCorrectSubmissionsAllPass() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            List<String> failures = new ArrayList<>();
            for (int round = 0; round < ROUNDS; round++) {
                CyclicBarrier barrier = new CyclicBarrier(THREADS);
                List<Future<SubmitResult>> futures = new ArrayList<>();
                for (int t = 0; t < THREADS; t++) {
                    futures.add(pool.submit(() -> {
                        barrier.await();
                        return controller.submit(new SqlRequest("basic-001",
                            "SELECT name, department FROM employees"));
                    }));
                }
                for (Future<SubmitResult> f : futures) {
                    SubmitResult r = f.get();
                    if (!r.correct()) failures.add(r.feedback());
                }
            }
            assertThat(failures).isEmpty();
        } finally {
            pool.shutdownNow();
        }
    }
}
