package ee.metsmarko.tuum.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springdoc.core.GroupedOpenApi;

class ConfigTest {

  private final Config config = new Config();

  @Test
  void swagger() {
    GroupedOpenApi swagger = config.swagger();

    assertEquals("Tuum", swagger.getGroup());
    assertEquals("/api/**", swagger.getPathsToMatch().get(0));
  }
}
