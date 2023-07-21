package dacite.lsp;

import dacite.lsp.tvp.DaciteTreeViewService;
import org.eclipse.lsp4j.services.LanguageClient;

public interface DaciteExtendedLanguageClient extends LanguageClient, DaciteTreeViewService {
}
