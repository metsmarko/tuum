package ee.metsmarko.tuum.account.event;

import ee.metsmarko.tuum.account.TransactionDirection;
import java.math.BigDecimal;
import java.util.UUID;

public record TransactionCreateEvent(UUID transactionId, UUID accountId, BigDecimal amount,
                                     String currency, TransactionDirection direction) {
}
