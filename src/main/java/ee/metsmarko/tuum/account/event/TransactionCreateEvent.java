package ee.metsmarko.tuum.account.event;

import java.util.UUID;

public record TransactionCreateEvent(UUID transactionId, UUID accountId) {
}
