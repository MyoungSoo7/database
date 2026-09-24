package lemuel.com.database.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 운영은 SPRING_PROFILES_ACTIVE=prod 다. 예전엔 활성 프로필 이름으로 checkPerProfile 을 찾아
 * "prod" 가 어느 키에도 안 걸렸다. 프로필 이름과 무관하게 실제 DB 로 골라야 한다.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:dialect-prod;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
@ActiveProfiles("prod")
class SqlDialectTest {

    @Autowired
    private SqlValidationService sqlValidationService;

    @Test
    void dialectComesFromTheDatabaseNotTheProfileName() {
        assertThat(sqlValidationService.dialect()).isEqualTo("h2");
    }

    @Test
    void productNamesMapToCheckPerProfileKeys() {
        assertThat(SqlValidationService.dialectOf("MySQL")).isEqualTo("mysql");
        assertThat(SqlValidationService.dialectOf("MariaDB")).isEqualTo("mysql");
        assertThat(SqlValidationService.dialectOf("PostgreSQL")).isEqualTo("postgres");
        assertThat(SqlValidationService.dialectOf("H2")).isEqualTo("h2");
    }
}
