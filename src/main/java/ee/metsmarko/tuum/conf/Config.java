package ee.metsmarko.tuum.conf;

import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {
  @Bean
  public GroupedOpenApi swagger() {
    return GroupedOpenApi.builder()
        .group("Tuum")
        .pathsToMatch("/api/**")
        .build();
  }
}
