package com.example.exchange.integration.concurrency;

import com.example.exchange.integration.config.TestContainersConfig;
import com.example.exchange.repository.ExchangeRepository;
import com.example.exchange.repository.UserBalanceRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
@WireMockTest(httpPort = 8099)
class ExchangeConcurrencyIT {

    private static final String TEST_USER = "concurrency-test-user";
    private static final String RATE_RESPONSE = """
            {
              "base": "USD",
              "date": "2024-01-01",
              "rates": { "EUR": 0.850000 }
            }
            """;

    @LocalServerPort
    private int port;

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Autowired
    private ExchangeRepository exchangeRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        stubFor(get(urlPathEqualTo("/USD"))
                .willReturn(okJson(RATE_RESPONSE)));

        depositBalance(TEST_USER, "USD", 100.00);
    }

    @AfterEach
    void cleanUp() {
        exchangeRepository.findByUserId(TEST_USER)
                .forEach(e -> exchangeRepository.deleteById(e.getId()));
        userBalanceRepository.findByUserId(TEST_USER)
                .forEach(b -> userBalanceRepository.deleteById(b.getId()));
    }

    @Test
    void shouldPreventDoubleSpendUnderConcurrentExchanges() throws Exception {
        int threads = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        List<Future<Response>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                startLatch.await();
                return given()
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                  "userId": "%s",
                                  "fromCurrency": "USD",
                                  "toCurrency": "EUR",
                                  "amount": 70.00
                                }
                                """.formatted(TEST_USER))
                        .when()
                        .post("/api/v1/exchanges")
                        .andReturn();
            }));
        }

        startLatch.countDown();
        executor.shutdown();

        List<Integer> statusCodes = new ArrayList<>();
        for (Future<Response> f : futures) {
            statusCodes.add(f.get().statusCode());
        }

        long successCount = statusCodes.stream().filter(code -> code == 201).count();
        long failureCount = statusCodes.stream().filter(code -> code >= 400).count();

        assertThat(successCount).isEqualTo(1);
        assertThat(failureCount).isEqualTo(1);

        BigDecimal finalBalance = userBalanceRepository
                .findByUserIdAndCurrency(TEST_USER, "USD")
                .map(b -> b.getBalance())
                .orElse(BigDecimal.ZERO);
        assertThat(finalBalance).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    void shouldHandleConcurrentDepositsCorrectly() throws Exception {
        depositBalance(TEST_USER, "EUR", 0.01);

        int threads = 5;
        BigDecimal depositAmount = new BigDecimal("20.00");
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        List<Future<Response>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                startLatch.await();
                return given()
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                  "userId": "%s",
                                  "currency": "EUR",
                                  "amount": 20.00
                                }
                                """.formatted(TEST_USER))
                        .when()
                        .post("/api/v1/balances/deposit")
                        .andReturn();
            }));
        }

        startLatch.countDown();
        executor.shutdown();

        for (Future<Response> f : futures) {
            assertThat(f.get().statusCode()).isEqualTo(201);
        }

        BigDecimal seed = new BigDecimal("0.01");
        BigDecimal expectedTotal = seed.add(depositAmount.multiply(BigDecimal.valueOf(threads)));
        BigDecimal actualBalance = userBalanceRepository
                .findByUserIdAndCurrency(TEST_USER, "EUR")
                .map(b -> b.getBalance())
                .orElse(BigDecimal.ZERO);

        assertThat(actualBalance).isEqualByComparingTo(expectedTotal);
    }

    private void depositBalance(String userId, String currency, double amount) {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "userId": "%s",
                          "currency": "%s",
                          "amount": %s
                        }
                        """.formatted(userId, currency, amount))
        .when()
                .post("/api/v1/balances/deposit")
        .then()
                .statusCode(201);
    }
}
