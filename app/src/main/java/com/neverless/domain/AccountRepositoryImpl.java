package com.neverless.domain;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AccountRepositoryImpl implements AccountRepository {

	private final Map<AccountId, Account> accounts = new ConcurrentHashMap<>();

	@Override
	public Optional<Account> find(AccountId id) {
		return Optional.ofNullable(accounts.get(id));
	}

	@Override
	public void addAccount(Account account) {
		accounts.put(account.id(), account);
	}
}
