package com.neverless.domain;

import java.math.BigDecimal;

public class AccountImpl implements Account {

	private final AccountId id;
	private BigDecimal balance;

	public AccountImpl(AccountId id, BigDecimal balance) {
		this.id = id;
		this.balance = balance;
	}

	@Override
	public AccountId id() {
		return id;
	}

	@Override
	public synchronized BigDecimal availableBalance() {
		return balance;
	}

	@Override
	public synchronized boolean reserveBalance(BigDecimal amount) {
		if (balance.compareTo(amount) < 0) {
			return false;
		}
		balance = balance.subtract(amount);
		return true;
	}

	@Override
	public synchronized void releaseBalance(BigDecimal amount) {
		balance = balance.add(amount);
	}
}
