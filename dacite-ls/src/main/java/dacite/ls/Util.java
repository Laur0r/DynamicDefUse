package dacite.ls;

import org.eclipse.lsp4j.services.LanguageClient;

public class Util {

  private static LanguageClient client;

  public static LanguageClient getClient() {
    return client;
  }

  public static void setClient(LanguageClient client) {
    Util.client = client;
  }

}
