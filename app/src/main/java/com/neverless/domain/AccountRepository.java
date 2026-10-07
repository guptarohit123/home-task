package com.neverless.domain;

import java.math.BigDecimal;
import java.util.Optional;

public interface AccountRepository {

	Optional<Account> find(AccountId id);

	void addAccount(Account account);
}
