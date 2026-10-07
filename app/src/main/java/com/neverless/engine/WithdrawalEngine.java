package com.neverless.engine;

import java.math.BigDecimal;

import com.neverless.domain.WithdrawalRequestStatus;
import com.neverless.domain.WithdrawalResponse;

public interface WithdrawalEngine {

	WithdrawalResponse withdraw(String withdrawalId, String accountId, BigDecimal amount);

	WithdrawalRequestStatus queryWithdrawalStatus(String withdrawalId);
}
