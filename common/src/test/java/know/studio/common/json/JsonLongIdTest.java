package know.studio.common.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonLongIdTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesOnlyAnnotatedIdentifiersAsStrings() throws Exception {
        String json = objectMapper.writeValueAsString(new IdentifierPayload(
                351734627792060416L,
                List.of(351734627792060417L),
                2048L
        ));

        assertThat(json).isEqualTo(
                "{\"id\":\"351734627792060416\",\"relatedIds\":[\"351734627792060417\"],\"fileSize\":2048}"
        );
    }

    private record IdentifierPayload(
            @JsonLongId long id,
            @JsonLongIds List<Long> relatedIds,
            long fileSize
    ) {
    }
}
