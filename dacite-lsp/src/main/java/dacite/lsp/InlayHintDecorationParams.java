package dacite.lsp;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.xtext.xbase.lib.Pure;
import org.eclipse.xtext.xbase.lib.util.ToStringBuilder;

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

    @Override
    @Pure
    @SuppressWarnings("UnstableApiUsage")
    public String toString() {
        ToStringBuilder b = new ToStringBuilder(this);
        b.add("identifier", this.identifier);
        b.add("position", this.position);
        return b.toString();
    }

}
