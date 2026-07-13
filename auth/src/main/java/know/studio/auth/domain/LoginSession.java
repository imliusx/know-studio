package know.studio.auth.domain;

public interface LoginSession {

    void login(long userId);

    void logout();

    boolean isLoggedIn();

    long currentUserId();

    String tokenName();

    String tokenValue();
}
