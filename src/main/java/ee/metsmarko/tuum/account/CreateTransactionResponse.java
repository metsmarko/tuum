package ee.metsmarko.tuum.account;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransactionResponse(UUID transactionId, UUID accountId, BigDecimal amount,
                                        String currency,
                                        TransactionDirection direction, String description,
                                        BigDecimal currentBalance) {
}
