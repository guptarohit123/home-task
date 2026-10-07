package com.neverless.engine;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.neverless.domain.Account;
import com.neverless.domain.AccountId;
import com.neverless.domain.AccountRepository;
import com.neverless.domain.WithdrawalRequestStatus;
import com.neverless.domain.WithdrawalResponse;
import com.neverless.integration.WithdrawalService;

public class WithdrawalEngineImpl implements WithdrawalEngine {

	private final AccountRepository accountRepo;
	private final Map<String, WithdrawalContext> withdrawalsRepository = new ConcurrentHashMap<>();
	private final WithdrawalService<BigDecimal> withdrawalService;
	private final ExecutorService remoteExecutor = Executors.newCachedThreadPool();
	private final ExecutorService controlThread = Executors.newSingleThreadExecutor();

	public WithdrawalEngineImpl(AccountRepository accountRepo, WithdrawalService<BigDecimal> withdrawalService) {
		this.accountRepo = accountRepo;
		this.withdrawalService = withdrawalService;
	}

	void control() {

	}

	record WithdrawalContext(String withdrawalId, WithdrawalService.WithdrawalId externalWithdrawalid, AccountId accountId, BigDecimal amount) {
	}

	@Override
	public WithdrawalResponse withdraw(String withdrawalId, String accountId, BigDecimal amount) {
		try {
			if (withdrawalId == null) {
				return WithdrawalResponse.ERROR;
			}
			if (withdrawalsRepository.containsKey(withdrawalId)) {
				return WithdrawalResponse.WITHDRAWAL_ID_EXISTS;
			}
			final Optional<Account> account = accountRepo.find(AccountId.fromString(accountId));
			if (account.isEmpty()) {
				return WithdrawalResponse.ACCOUNT_NOT_FOUND;
			}
			final Account acc = account.get();
			if (acc.reserveBalance(amount)) {
				UUID withhdrawalUuid = UUID.randomUUID();
				WithdrawalService.WithdrawalId withdrawalIdExternal = new WithdrawalService.WithdrawalId(withhdrawalUuid);
				WithdrawalService.Address address = new WithdrawalService.Address(accountId);
				final WithdrawalContext prev = withdrawalsRepository.putIfAbsent(withdrawalId,
						new WithdrawalContext(withdrawalId, withdrawalIdExternal, acc.id(), amount));
				if (prev != null) {
					acc.releaseBalance(amount);
					return WithdrawalResponse.WITHDRAWAL_ID_EXISTS;
				}
				remoteExecutor.submit(() -> {
					withdrawalService.requestWithdrawal(withdrawalIdExternal, address, amount);
				});
				return WithdrawalResponse.SUBMITTED;
			} else {
				return WithdrawalResponse.INSUFFICIENT_FUNDS;
			}
		} catch (Exception e) {
			System.out.println("error processing withdrawal request " + withdrawalId + e.getMessage());
		}
		return WithdrawalResponse.ERROR;
	}

	@Override
	public WithdrawalRequestStatus queryWithdrawalStatus(String withdrawalId) {
		return null;
	}
}
