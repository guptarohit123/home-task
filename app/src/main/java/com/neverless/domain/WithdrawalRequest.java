package com.neverless.domain;

import java.math.BigDecimal;

public record WithdrawalRequest(String withdrawalId, String accountId , BigDecimal amount) {
}
