package com.neverless.domain;

import java.math.BigDecimal;
import java.util.UUID;

public interface Account {

	AccountId id();

	BigDecimal availableBalance();

	boolean reserveBalance(BigDecimal amount);

	void releaseBalance(BigDecimal amount);

}
