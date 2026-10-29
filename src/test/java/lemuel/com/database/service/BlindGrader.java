package lemuel.com.database.service;

import org.yaml.snakeyaml.Yaml;
import lemuel.com.database.dto.SubmitResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 문제 작성 도구. 설명만 보고 푼 답안(JSON {id: sql})을 실제 채점기로 채점해 build/gen/blind.txt 에 적는다.
 * 설명이 모호하면 여기서 떨어진다. BLIND_ANSWERS=경로 SPRING_DATASOURCE_URL=jdbc:mysql://... 로 돈다.
 */
@SpringBootTest
@ActiveProfiles("mysql")
@EnabledIfEnvironmentVariable(named = "BLIND_ANSWERS", matches = ".+")
class BlindGrader {

    @Autowired
    private SqlValidationService validation;

    @Test
    @SuppressWarnings("unchecked")
    void grade() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (String file : System.getenv("BLIND_ANSWERS").split(",")) {
            Map<String, String> answers = (Map<String, String>) new Yaml().load(Files.readString(Path.of(file)));
            for (Map.Entry<String, String> e : answers.entrySet()) {
                SubmitResult r = validation.validate(e.getKey(), e.getValue());
                sb.append(r.correct() ? "PASS " : "FAIL ").append(e.getKey());
                if (!r.correct()) {
                    sb.append(" | ").append(r.feedback().replace('\n', ' '));
                    if (r.userResult() != null) {
                        sb.append(" | got=").append(r.userResult().rows());
                    }
                }
                sb.append('\n');
            }
        }
        Files.createDirectories(Path.of("build/gen"));
        Files.writeString(Path.of("build/gen/blind.txt"), sb.toString());
    }
}
