package know.studio.auth.domain;

import know.studio.auth.api.CurrentIdentity;

public record AuthSession(CurrentIdentity user, String tokenName, String tokenValue) {
}
