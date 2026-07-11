package know.studio.arag.identity.domain;

import know.studio.arag.identity.api.WorkspaceRole;

public record WorkspaceMember(UserAccount user, WorkspaceRole role) {
}
