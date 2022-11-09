package dacite.lsp;

import org.eclipse.lsp4j.InlayHintParams;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;

import java.util.concurrent.CompletableFuture;

@JsonSegment("dacite")
public interface DaciteExtendedTextDocumentService {

  @JsonRequest
  CompletableFuture<InlayHintDecoration> inlayHintDecoration(InlayHintDecorationParams params);

}
