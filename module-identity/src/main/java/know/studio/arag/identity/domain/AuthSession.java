package know.studio.arag.identity.domain;

import know.studio.arag.identity.api.CurrentIdentity;

public record AuthSession(CurrentIdentity user, String tokenName, String tokenValue) {
}
