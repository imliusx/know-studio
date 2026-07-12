package know.studio.arag.knowledge.domain;

import know.studio.arag.identity.api.CurrentIdentity;
import know.studio.arag.identity.api.IdentityApi;
import know.studio.arag.identity.api.SystemRole;
import know.studio.arag.knowledge.api.KnowledgeBasePermission;
import know.studio.arag.knowledge.api.KnowledgeBaseVisibility;
import know.studio.arag.platform.core.exception.ForbiddenException;
import know.studio.arag.platform.core.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeBaseServiceTest {

    @Test
    void combinesCompanyAndTeamGrantedKnowledgeBases() {
        KnowledgeBaseRepository repository = mock(KnowledgeBaseRepository.class);
        IdentityApi identityApi = mock(IdentityApi.class);
        when(identityApi.currentUser()).thenReturn(user(10L, SystemRole.USER));
        when(identityApi.currentUserTeamIds()).thenReturn(Set.of(20L));
        when(repository.findPermissions(Set.of(20L)))
                .thenReturn(Map.of(2L, KnowledgeBasePermission.READ));
        when(repository.findCompanyVisible()).thenReturn(List.of(kb(1L, KnowledgeBaseVisibility.COMPANY, 1L)));
        when(repository.findCreatedBy(10L)).thenReturn(List.of());
        KnowledgeBaseService service = service(repository, identityApi);

        assertThat(service.readableKnowledgeBaseIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void readGrantCannotManageKnowledgeBase() {
        KnowledgeBaseRepository repository = mock(KnowledgeBaseRepository.class);
        IdentityApi identityApi = mock(IdentityApi.class);
        when(identityApi.currentUser()).thenReturn(user(10L, SystemRole.USER));
        when(identityApi.currentUserTeamIds()).thenReturn(Set.of(20L));
        when(repository.findById(2L)).thenReturn(Optional.of(kb(2L, KnowledgeBaseVisibility.TEAM, 99L)));
        when(repository.findPermission(2L, Set.of(20L)))
                .thenReturn(Optional.of(KnowledgeBasePermission.READ));
        KnowledgeBaseService service = service(repository, identityApi);

        assertThatThrownBy(() -> service.requireManageable(2L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("权限不足");
    }

    @Test
    void systemAdminCanReadEveryActiveKnowledgeBase() {
        KnowledgeBaseRepository repository = mock(KnowledgeBaseRepository.class);
        IdentityApi identityApi = mock(IdentityApi.class);
        when(identityApi.currentUser()).thenReturn(user(1L, SystemRole.ADMIN));
        when(repository.findAllActive()).thenReturn(List.of(
                kb(1L, KnowledgeBaseVisibility.COMPANY, 2L),
                kb(2L, KnowledgeBaseVisibility.PRIVATE, 3L)
        ));
        KnowledgeBaseService service = service(repository, identityApi);

        assertThat(service.readableKnowledgeBaseIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    private static KnowledgeBaseService service(
            KnowledgeBaseRepository repository,
            IdentityApi identityApi
    ) {
        return new KnowledgeBaseService(repository, identityApi, new SnowflakeIdGenerator(0, 0));
    }

    private static CurrentIdentity user(long userId, SystemRole role) {
        return new CurrentIdentity(userId, "user@example.com", "User", role);
    }

    private static KnowledgeBase kb(long id, KnowledgeBaseVisibility visibility, long createdBy) {
        return new KnowledgeBase(id, "KB " + id, null, visibility, null, createdBy, "ACTIVE");
    }
}
