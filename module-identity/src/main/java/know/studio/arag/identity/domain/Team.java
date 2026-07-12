package know.studio.arag.identity.domain;

public record Team(
        long id,
        String name,
        String description,
        Long parentId,
        long createdBy,
        TeamStatus status
) {
}
