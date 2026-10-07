package com.neverless.resources;

import com.neverless.domain.AccountRepository;
import com.neverless.exceptions.NotFoundException;
import io.javalin.router.JavalinDefaultRouting;

public class Resources {
	private final Healthcheck healthcheck;
	private final Accounts accounts;
	private final WithdrawalProcessor withdrawalProcessor;

	public Resources(AccountRepository accountRepo) {
		healthcheck = new Healthcheck();
		accounts = new Accounts(accountRepo);
		withdrawalProcessor = new WithdrawalProcessor(accountRepo);
	}

	public void register(JavalinDefaultRouting router) {
		router.exception(NotFoundException.class, (ex, ctx) -> ctx.status(404));

		router.get("/healthcheck", healthcheck::check);
		router.get("/accounts/{id}", accounts::get);
		router.post("/withdrawals/startWithdrawal", withdrawalProcessor::startWithdrawalRequest);
		router.post("/withdrawals/queryWithdrawalStatus", withdrawalProcessor::queryWithdrawalRequest);
	}
}
