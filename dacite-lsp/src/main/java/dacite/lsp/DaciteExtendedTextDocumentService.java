package dacite.lsp;

import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;

import java.util.concurrent.CompletableFuture;

@JsonSegment("dacite")
public interface DaciteExtendedTextDocumentService {

  @JsonRequest
  CompletableFuture<Object> inlayHintDecoration(Object params);

}
