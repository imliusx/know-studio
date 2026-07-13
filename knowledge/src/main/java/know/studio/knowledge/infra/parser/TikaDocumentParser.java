package know.studio.knowledge.infra.parser;

import know.studio.knowledge.domain.DocumentParserPort;
import know.studio.knowledge.domain.DocumentRecord;
import know.studio.knowledge.domain.ObjectStoragePort;
import know.studio.knowledge.domain.ParsedDocument;
import lombok.RequiredArgsConstructor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class TikaDocumentParser implements DocumentParserPort {

    private final ObjectStoragePort storage;

    @Override
    public ParsedDocument parse(DocumentRecord document) {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, document.fileName());
        try (InputStream input = storage.open(document.objectKey())) {
            parser.parse(input, handler, metadata, new ParseContext());
            return new ParsedDocument(handler.toString(), metadata.get(Metadata.CONTENT_TYPE));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse document " + document.id(), exception);
        }
    }
}
