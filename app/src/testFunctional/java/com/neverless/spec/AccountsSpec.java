package com.neverless.spec;

import com.neverless.domain.AccountId;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

public class AccountsSpec extends FunctionalSpec {

	protected AccountsSpec(ApplicationContext context) {
		super(context);
	}

	@Test
    void should_respond_with_account_on_accounts_get_when_exists() throws Exception {
        final var id = AccountId.random();

        // when
        final var response = when().get("/accounts/{id}", id.value()).thenReturn();

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThatJson(response.body().asString()).isEqualTo(
            """
            {
                "id": "%s"
            }
            """.formatted(id.value())
        );
    }

	@Test
	void should_respond_with_404_on_accounts_get_when_not_exists() throws Exception {
		final var id = AccountId.random();

		// when
		final var response = when().get("/accounts/{id}", id.value()).thenReturn();

		// then
		assertThat(response.statusCode()).isEqualTo(404);
	}
}
