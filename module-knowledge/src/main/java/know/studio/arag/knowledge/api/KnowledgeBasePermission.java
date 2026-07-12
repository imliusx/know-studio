package know.studio.arag.knowledge.api;

public enum KnowledgeBasePermission {
    READ(10),
    MANAGE(20);

    private final int authority;

    KnowledgeBasePermission(int authority) {
        this.authority = authority;
    }

    public boolean allows(KnowledgeBasePermission required) {
        return authority >= required.authority;
    }
}
