package know.studio.agent.api;

public record ChatStreamEvent(Type type, Object payload) {

    public enum Type {
        TOKEN("token"),
        THINKING("thinking"),
        TOOL_CALL("tool_call"),
        TOOL_RESULT("tool_result"),
        CITATION("citation"),
        DONE("done"),
        ERROR("error");

        private final String eventName;

        Type(String eventName) {
            this.eventName = eventName;
        }

        public String eventName() {
            return eventName;
        }
    }

    public static ChatStreamEvent token(String content) {
        return new ChatStreamEvent(Type.TOKEN, content);
    }

    public static ChatStreamEvent thinking(String content) {
        return new ChatStreamEvent(Type.THINKING, content);
    }
}
