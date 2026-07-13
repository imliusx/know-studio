package know.studio.auth.api;

public enum TeamRole {
    MEMBER(10),
    TEAM_ADMIN(20);

    private final int authority;

    TeamRole(int authority) {
        this.authority = authority;
    }

    public boolean allows(TeamRole required) {
        return authority >= required.authority;
    }
}
