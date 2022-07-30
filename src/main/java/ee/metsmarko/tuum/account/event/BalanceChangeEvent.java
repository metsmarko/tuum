package ee.metsmarko.tuum.account.event;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceChangeEvent(UUID accountId, String currency, BigDecimal amount,
                                 BigDecimal newBalance) {
}
