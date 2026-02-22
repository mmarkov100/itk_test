package com.test.itk;

import com.test.itk.dto.WalletOperationRequest;
import com.test.itk.entity.OperationType;
import com.test.itk.entity.Wallet;
import com.test.itk.repository.WalletRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WalletIntegrationTest {

    // Контейнер должен быть статическим и помечен @Container
    // Testcontainers инициализирует его ДО загрузки контекста Spring
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("wallet")
            .withUsername("wallet")
            .withPassword("wallet");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        // Теперь контейнер уже запущен к моменту вызова этого метода
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WalletRepository walletRepository;

    private UUID walletId;

    @BeforeEach
    void setup() {
        // Создаем кошелек перед каждым тестом
        walletId = UUID.randomUUID();
        Wallet wallet = new Wallet(walletId, BigDecimal.valueOf(1000));
        walletRepository.save(wallet);
    }

    @Test
    void testDeposit() {
        WalletOperationRequest request = new WalletOperationRequest(
                walletId,
                OperationType.DEPOSIT,
                BigDecimal.valueOf(500)
        );

        restTemplate.postForEntity(
                "http://localhost:" + port + "/api/v1/wallet",
                request,
                String.class
        );

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1500));
    }

    @Test
    void testWithdraw() {
        WalletOperationRequest request = new WalletOperationRequest(
                walletId,
                OperationType.WITHDRAW,
                BigDecimal.valueOf(300)
        );

        restTemplate.postForEntity(
                "http://localhost:" + port + "/api/v1/wallet",
                request,
                String.class
        );

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(700));
    }

    @Test
    void testInsufficientFunds() {
        WalletOperationRequest request = new WalletOperationRequest(
                walletId,
                OperationType.WITHDRAW,
                BigDecimal.valueOf(2000)
        );

        var response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/v1/wallet",
                request,
                String.class
        );

        Assertions.assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void testWalletNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        WalletOperationRequest request = new WalletOperationRequest(
                nonExistentId,
                OperationType.DEPOSIT,
                BigDecimal.valueOf(100)
        );

        var response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/v1/wallet",
                request,
                String.class
        );

        Assertions.assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void testGetBalance() {
        var response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/wallets/" + walletId,
                String.class
        );

        Assertions.assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testConcurrentDeposits() throws InterruptedException {
        int threads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                WalletOperationRequest req = new WalletOperationRequest(
                        walletId,
                        OperationType.DEPOSIT,
                        BigDecimal.valueOf(10)
                );
                restTemplate.postForEntity(
                        "http://localhost:" + port + "/api/v1/wallet",
                        req,
                        String.class
                );
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        // Начальный баланс 1000 + 50 * 10 = 1500
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1500));
    }
}