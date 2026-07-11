package know.studio.arag.identity.domain;

public interface LoginSession {

    void login(long userId);

    void logout();

    boolean isLoggedIn();

    long currentUserId();

    String tokenName();

    String tokenValue();
}
