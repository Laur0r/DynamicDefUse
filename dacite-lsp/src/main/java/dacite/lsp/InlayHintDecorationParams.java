package dacite.lsp;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;

public class InlayHintDecorationParams {

    private TextDocumentIdentifier identifier;
    private Position position;

    public InlayHintDecorationParams(TextDocumentIdentifier identifier, Position pos){
        this.identifier = identifier;
        this.position = pos;
    }

    public Position getPosition() {
        return position;
    }
    public TextDocumentIdentifier getIdentifier() {
        return identifier;
    }
}
