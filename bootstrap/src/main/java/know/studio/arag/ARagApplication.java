package know.studio.arag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Agentic RAG 平台后端启动类
 *
 * <p>{@code @SpringBootApplication} 默认扫描 {@code know.studio.arag} 及其子包，
 * 因此各业务/平台模块的 {@code know.studio.arag.*} 组件都会被自动装配。
 */
@SpringBootApplication
public class ARagApplication {

    public static void main(String[] args) {
        SpringApplication.run(ARagApplication.class, args);
    }
}
