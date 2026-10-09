package com.neverless.engine;

import com.neverless.domain.*;
import com.neverless.integration.WithdrawalService;
import com.neverless.integration.WithdrawalService.WithdrawalState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class WithdrawalEngineImpl implements WithdrawalEngine {

    private static final Logger log = LoggerFactory.getLogger(WithdrawalEngineImpl.class);

    private final AccountRepository accountRepo;
    private final Map<String, WithdrawalContext> withdrawalsRepository;
    private final WithdrawalService<BigDecimal> withdrawalService;
    private final Clock clock;
    private final ScheduledExecutorService controlExecutor = Executors.newSingleThreadScheduledExecutor();


    public WithdrawalEngineImpl(AccountRepository accountRepo, WithdrawalService<BigDecimal> withdrawalService) {
        this.accountRepo = accountRepo;
        this.withdrawalService = withdrawalService;
        this.clock = Clock.systemUTC();
        this.withdrawalsRepository = new ConcurrentHashMap<>();
    }

    public void init() {
        this.controlExecutor.scheduleAtFixedRate(this::control, 10, 10, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(this.controlExecutor::shutdownNow));
    }

    private void control() {
        long now = clock.millis();
        withdrawalsRepository.values().forEach((ctx) -> {

            WithdrawalState requestState;
            try {
                requestState = withdrawalService.getRequestState(ctx.externalWithdrawalid);
            } catch (Exception e) {
                log.error("Error trying to get request state for id = {}", ctx.withdrawalId, e);
                // Do not expect this to happen - but if it does recovery?
                return;
            }
            if (requestState != WithdrawalState.PROCESSING) {
                ctx.finalizationTime.set(now);
                ctx.withdrawalStateReference.set(requestState);
                if (requestState == WithdrawalState.FAILED) {
                    Optional<Account> account = accountRepo.find(ctx.accountId);
                    if (account.isPresent()) {
                        account.get().releaseBalance(ctx.amount);
                        log.info("Withdrawal failed for account = {} , withdrawal id = {} , withdrawal amount = {}. Restoring account balance", ctx.accountId.toString(),
                                ctx.withdrawalId, ctx.amount);
                    } else {
                        log.error("Withdrawal failed for account = {} , withdrawal id = {} , withdrawal amount = {}. Coul not retrieve account to restore balance", ctx.accountId.toString(),
                                ctx.withdrawalId, ctx.amount);
                    }
                }
                log.info("Withdrawal finalized for withdrawal context {}", ctx);
                // retention policy for withdrawal repository to be fixed in the next iteration, POC will retain keys indefinitely
            }
        });
    }

    record WithdrawalContext(String withdrawalId,
                             WithdrawalService.WithdrawalId externalWithdrawalid,
                             AccountId accountId,
                             BigDecimal amount,
                             long creationTime,
                             AtomicLong finalizationTime,
                             AtomicReference<WithdrawalService.WithdrawalState> withdrawalStateReference) {
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
                    new WithdrawalContext(withdrawalId,
                            withdrawalIdExternal,
                            acc.id(),
                            amount,
                            clock.millis(),
                            new AtomicLong(),
                            new AtomicReference<>()));
            if (prev != null) {
                return WithdrawalResponse.WITHDRAWAL_ID_EXISTS;
            }
            if (acc.reserveBalance(amount)) {
                withdrawalService.requestWithdrawal(withdrawalIdExternal, address, amount);
                return WithdrawalResponse.SUBMITTED;
            } else {
                withdrawalsRepository.remove(withdrawalId);
                return WithdrawalResponse.INSUFFICIENT_FUNDS;
            }
        } catch (Exception e) {
            log.error("Error processing withdrawal id={}", withdrawalId, e);
        }
        return WithdrawalResponse.ERROR;
    }

    @Override
    public WithdrawalRequestStatus queryWithdrawalStatus(String withdrawalId) {
        final WithdrawalContext withdrawalContext = withdrawalsRepository.get(withdrawalId);
        if (withdrawalContext == null) {
            return WithdrawalRequestStatus.REQUEST_NOT_FOUND;
        }
        try {
            WithdrawalState requestState = withdrawalService.getRequestState(withdrawalContext.externalWithdrawalid);
            switch (requestState) {
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
            log.error("Error querying withdrawal status for id {}", withdrawalId, e);
        }

        return WithdrawalRequestStatus.FAILED;
    }
}
