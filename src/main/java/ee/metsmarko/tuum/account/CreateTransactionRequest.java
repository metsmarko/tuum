package ee.metsmarko.tuum.account;

import java.math.BigDecimal;

public record CreateTransactionRequest(BigDecimal amount, String currency,
                                       TransactionDirection direction, String description) {
}
