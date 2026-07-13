package know.studio.knowledge.domain;

public interface DocumentParserPort {

    ParsedDocument parse(DocumentRecord document);
}
