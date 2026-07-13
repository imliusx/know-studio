package know.studio.auth.domain;

public record Team(
        long id,
        String name,
        String description,
        Long parentId,
        long createdBy,
        TeamStatus status
) {
}
