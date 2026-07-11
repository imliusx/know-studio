package know.studio.arag.knowledge.domain;

public interface DocumentParserPort {

    ParsedDocument parse(DocumentRecord document);
}
