package know.studio.arag.identity.api;

public enum WorkspaceRole {
    MEMBER(10),
    ADMIN(20),
    OWNER(30);

    private final int authority;

    WorkspaceRole(int authority) {
        this.authority = authority;
    }

    public boolean allows(WorkspaceRole required) {
        return authority >= required.authority;
    }
}
