package know.studio.ai.routing;

import java.io.Serial;

public class AiRoutingException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AiRoutingException(String message) {
        super(message);
    }
}
