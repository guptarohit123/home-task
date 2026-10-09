package com.neverless.engine;

import com.neverless.domain.*;
import com.neverless.integration.WithdrawalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class WithdrawalEngineImplTest {

	private static final UUID TEST_ACC = UUID.randomUUID();
	private WithdrawalEngine _sut;

	@Mock
	private WithdrawalService<BigDecimal> withdrawalService;

	@Captor
	ArgumentCaptor<WithdrawalService.WithdrawalId> withdrawalIdArgCaptor;
	@Captor
	ArgumentCaptor<WithdrawalService.Address> addressArgCaptor;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
		AccountRepository accountRepository = new AccountRepositoryImpl();
		Account testAccount = new AccountImpl(AccountId.of(TEST_ACC), BigDecimal.valueOf(100));
		accountRepository.addAccount(testAccount);
		_sut = new WithdrawalEngineImpl(accountRepository, withdrawalService);
		_sut.init();
	}

	@Test
	void withdrawal_happy_scenario() {
		final String withdrawalId = "testId1";
		final WithdrawalResponse response = _sut.withdraw(withdrawalId, TEST_ACC.toString(), BigDecimal.TEN);

		verify(withdrawalService).requestWithdrawal(withdrawalIdArgCaptor.capture(),
				addressArgCaptor.capture(),
				eq(BigDecimal.TEN));
		when(withdrawalService.getRequestState(withdrawalIdArgCaptor.getValue())).thenReturn(WithdrawalService.WithdrawalState.COMPLETED);

		assertEquals(WithdrawalResponse.SUBMITTED, response);

		final WithdrawalRequestStatus withdrawalRequestStatus = _sut.queryWithdrawalStatus(withdrawalId);
		assertEquals(WithdrawalRequestStatus.SUCCESS, withdrawalRequestStatus);
	}

	@Test
	void insufficient_account_balance() {
		final String withdrawalId = "testId1";
		final BigDecimal withdrawalAmount = BigDecimal.valueOf(100.01);
		final WithdrawalResponse response = _sut.withdraw(withdrawalId, TEST_ACC.toString(), withdrawalAmount);
		verify(withdrawalService, times(0)).requestWithdrawal(withdrawalIdArgCaptor.capture(),
				addressArgCaptor.capture(),
				eq(withdrawalAmount));
		assertEquals(WithdrawalResponse.INSUFFICIENT_FUNDS, response);
		final WithdrawalRequestStatus withdrawalRequestStatus = _sut.queryWithdrawalStatus(withdrawalId);
		assertEquals(WithdrawalRequestStatus.REQUEST_NOT_FOUND, withdrawalRequestStatus);
	}

	@Test
	void duplicate_withdrawalId_handling() {
		final String withdrawalId = "testId1";
		final BigDecimal withdrawalAmount = BigDecimal.valueOf(45);
		final WithdrawalResponse response1 = _sut.withdraw(withdrawalId, TEST_ACC.toString(), withdrawalAmount);
		final WithdrawalResponse response2 = _sut.withdraw(withdrawalId, TEST_ACC.toString(), withdrawalAmount);
		verify(withdrawalService, times(1)).requestWithdrawal(withdrawalIdArgCaptor.capture(),
				addressArgCaptor.capture(),
				eq(withdrawalAmount));
		assertEquals(WithdrawalResponse.SUBMITTED, response1);
		assertEquals(WithdrawalResponse.WITHDRAWAL_ID_EXISTS, response2);
		when(withdrawalService.getRequestState(withdrawalIdArgCaptor.getValue())).thenReturn(WithdrawalService.WithdrawalState.COMPLETED);
		final WithdrawalRequestStatus withdrawalRequestStatus = _sut.queryWithdrawalStatus(withdrawalId);
		assertEquals(WithdrawalRequestStatus.SUCCESS, withdrawalRequestStatus);
	}

	@Test
	void testWithdrawalFailed() {
		final String withdrawalId = "testId1";
		final BigDecimal withdrawalAmount = BigDecimal.valueOf(45);
		final WithdrawalResponse response = _sut.withdraw(withdrawalId, TEST_ACC.toString(), withdrawalAmount);
		verify(withdrawalService).requestWithdrawal(withdrawalIdArgCaptor.capture(),
				addressArgCaptor.capture(),
				eq(withdrawalAmount));
		when(withdrawalService.getRequestState(withdrawalIdArgCaptor.getValue())).thenReturn(WithdrawalService.WithdrawalState.FAILED);
		assertEquals(WithdrawalResponse.SUBMITTED, response);
		final WithdrawalRequestStatus withdrawalRequestStatus = _sut.queryWithdrawalStatus(withdrawalId);
		assertEquals(WithdrawalRequestStatus.FAILED, withdrawalRequestStatus);
	}

}