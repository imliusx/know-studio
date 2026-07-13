package know.studio.auth.infra;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import know.studio.auth.domain.LoginSession;
import org.springframework.stereotype.Component;

@Component
public class SaTokenLoginSession implements LoginSession {

    @Override
    public void login(long userId) {
        StpUtil.login(userId);
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public boolean isLoggedIn() {
        return StpUtil.isLogin();
    }

    @Override
    public long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Override
    public String tokenName() {
        return tokenInfo().getTokenName();
    }

    @Override
    public String tokenValue() {
        return tokenInfo().getTokenValue();
    }

    private static SaTokenInfo tokenInfo() {
        return StpUtil.getTokenInfo();
    }
}
