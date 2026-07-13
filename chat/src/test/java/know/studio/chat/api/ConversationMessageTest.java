package know.studio.chat.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationMessageTest {

    @Test
    void normalizesLegacyNumericCitationIdentifiers() {
        ConversationMessage message = new ConversationMessage(
                1L,
                MessageRole.ASSISTANT,
                "answer",
                1,
                Map.of(
                        "knowledgeBaseIds", List.of(351458513513349120L),
                        "citations", List.of(Map.of(
                                "knowledgeBaseId", 351458513513349120L,
                                "documentId", 351458714621837312L,
                                "chunkId", 351458715699773440L,
                                "chunkIndex", 0
                        ))
                ),
                Instant.EPOCH
        );

        assertThat(message.metadata().get("knowledgeBaseIds"))
                .isEqualTo(List.of("351458513513349120"));
        @SuppressWarnings("unchecked")
        Map<String, Object> citation = (Map<String, Object>)
                ((List<?>) message.metadata().get("citations")).getFirst();
        assertThat(citation)
                .containsEntry("knowledgeBaseId", "351458513513349120")
                .containsEntry("documentId", "351458714621837312")
                .containsEntry("chunkId", "351458715699773440")
                .containsEntry("chunkIndex", 0);
    }
}
