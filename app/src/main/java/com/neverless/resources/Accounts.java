package com.neverless.resources;

import com.neverless.domain.Account;
import com.neverless.domain.AccountId;
import com.neverless.domain.AccountRepository;
import com.neverless.exceptions.NotFoundException;
import io.javalin.http.Context;

public class Accounts {
	private final AccountRepository accountRepo;

	public Accounts(AccountRepository accountRepo) {
		this.accountRepo = accountRepo;
	}

	public void get(Context context) {
		final var id = AccountId.fromString(context.pathParam("id"));
		final var account = accountRepo.find(id).orElseThrow(() -> new NotFoundException("%s is not found".formatted(id)));

		context.json(AccountResponse.of(account));
	}

	public record AccountResponse(AccountId id) {
        public static AccountResponse of(Account account) {
            return new AccountResponse(account.id());
        }
    }
}
