package dacite.ls;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class DaciteLanguageServerTest extends AbstractLanguageServerTest {

  @Test
  public void testInitialization() throws Exception {
    var initializeResult = initializeLanguageServer();
    assertThat(this.languageServer).isNotNull();
    assertThat(initializeResult.getCapabilities().getTextDocumentSync()).isNotNull();
  }

}
