package ee.metsmarko.tuum.account;

import java.util.Set;
import java.util.UUID;

public record CreateAccountRequest(UUID customerId, String country, Set<String> currencies) {
}
