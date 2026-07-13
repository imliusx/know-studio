package know.studio.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Know Studio 应用启动类。
 *
 * <p>各业务模块位于 {@code know.studio} 下的同级包中，因此显式扫描整个基础包。
 */
@SpringBootApplication(scanBasePackages = "know.studio")
@EnableScheduling
public class KnowStudioApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowStudioApplication.class, args);
    }
}
