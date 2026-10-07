package com.neverless.resources;

import com.neverless.domain.AccountRepository;
import com.neverless.domain.WithdrawalRequest;
import com.neverless.domain.WithdrawalRequestStatus;
import com.neverless.domain.WithdrawalResponse;
import com.neverless.domain.WithdrawalStatusRequest;

import io.javalin.http.Context;

public class WithdrawalProcessor {

	private final AccountRepository accountRepository;

	public WithdrawalProcessor(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	public void startWithdrawalRequest(Context context) {
		final WithdrawalRequest withdrawalRequest = context.bodyAsClass(WithdrawalRequest.class);

	}

	public void queryWithdrawalRequest(Context context) {
		final WithdrawalStatusRequest withdrawalStatusRequest = context.bodyAsClass(WithdrawalStatusRequest.class);
	}

	public record WithdrawalRequestResponse(WithdrawalResponse response) {
	}

	public record WithdrawalRequestStatusResponse(WithdrawalRequestStatus status) {
	}
}
