package com.neverless.engine;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.neverless.domain.Account;
import com.neverless.domain.AccountId;
import com.neverless.domain.AccountRepository;
import com.neverless.domain.WithdrawalRequestStatus;
import com.neverless.domain.WithdrawalResponse;
import com.neverless.integration.WithdrawalService;
import com.neverless.integration.WithdrawalService.WithdrawalState;

public class WithdrawalEngineImpl implements WithdrawalEngine {

	private static final long WITHDRAWAL_REQUEST_TTL = TimeUnit.MINUTES.toMillis(5);

	private final AccountRepository accountRepo;
	private final Map<String, WithdrawalContext> withdrawalsRepository = new ConcurrentHashMap<>();
	private final WithdrawalService<BigDecimal> withdrawalService;
	private final Clock clock;
	private final ExecutorService remoteExecutor = Executors.newCachedThreadPool();
	private final ExecutorService controlThread = Executors.newSingleThreadExecutor();

	private volatile boolean terminated;

	public WithdrawalEngineImpl(AccountRepository accountRepo, WithdrawalService<BigDecimal> withdrawalService) {
		this.accountRepo = accountRepo;
		this.withdrawalService = withdrawalService;
		this.clock = Clock.systemUTC();
	}

	public void init() {
		this.controlThread.submit(this::control);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> terminated = true));
	}

	private void control() {
		while (!terminated) {
			long now = clock.millis();
			final int prev = withdrawalsRepository.size();
			withdrawalsRepository.values().removeIf(ctx -> ctx.creationTime - now >= WITHDRAWAL_REQUEST_TTL);
			System.out.println(String.format("pruned %s requests from repository", prev - withdrawalsRepository.size()));
		}

	}

	record WithdrawalContext(String withdrawalId,
			WithdrawalService.WithdrawalId externalWithdrawalid,
			AccountId accountId,
			BigDecimal amount,
			long creationTime) {
	}

	@Override
	public WithdrawalResponse withdraw(String withdrawalId, String accountId, BigDecimal amount) {
		try {
			if (withdrawalId == null) {
				return WithdrawalResponse.ERROR;
			}
			final Optional<Account> account = accountRepo.find(AccountId.fromString(accountId));
			if (account.isEmpty()) {
				return WithdrawalResponse.ACCOUNT_NOT_FOUND;
			}
			final Account acc = account.get();
			UUID withhdrawalUuid = UUID.randomUUID();
			WithdrawalService.WithdrawalId withdrawalIdExternal = new WithdrawalService.WithdrawalId(withhdrawalUuid);
			WithdrawalService.Address address = new WithdrawalService.Address(accountId);
			final WithdrawalContext prev = withdrawalsRepository.putIfAbsent(withdrawalId,
					new WithdrawalContext(withdrawalId, withdrawalIdExternal, acc.id(), amount, clock.millis()));
			if (prev != null) {
				return WithdrawalResponse.WITHDRAWAL_ID_EXISTS;
			}
			if (acc.reserveBalance(amount)) {
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
		final WithdrawalContext withdrawalContext = withdrawalsRepository.get(withdrawalId);
		if (withdrawalContext == null) {
			return WithdrawalRequestStatus.REQUEST_NOT_FOUND;
		}
		final Future<WithdrawalState> withdrawalStateFuture = remoteExecutor.submit(
				() -> withdrawalService.getRequestState(withdrawalContext.externalWithdrawalid));

		try {
			final WithdrawalState withdrawalState = withdrawalStateFuture.get(10, TimeUnit.SECONDS);
			switch (withdrawalState) {
			case PROCESSING -> {
				return WithdrawalRequestStatus.IN_PROGRESS;
			}
			case COMPLETED -> {
				return WithdrawalRequestStatus.SUCCESS;
			}
			case FAILED -> {
				return WithdrawalRequestStatus.FAILED;
			}
			}
		} catch (Exception e) {
			System.out.println("external service call failed for id -" + withdrawalId);
		}
		return WithdrawalRequestStatus.FAILED;
	}
}
