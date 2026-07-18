package ru.practicum.gateway.startup;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class RequiredServicesStartupWaiter {

    private static final String ENABLED_ENV =
            "GATEWAY_WAIT_FOR_REQUIRED_SERVICES";

    private static final String EUREKA_URL_ENV =
            "EUREKA_CLIENT_SERVICEURL_DEFAULTZONE";

    private static final String TIMEOUT_ENV =
            "GATEWAY_REQUIRED_SERVICES_TIMEOUT_SECONDS";

    private static final String DEFAULT_EUREKA_URL =
            "http://localhost:8761/eureka/";

    private static final int DEFAULT_TIMEOUT_SECONDS = 90;
    private static final int CHECK_INTERVAL_MILLIS = 1000;
    private static final int REQUEST_TIMEOUT_SECONDS = 2;

    private static final List<String> REQUIRED_SERVICES = List.of(
            "USER-SERVICE",
            "EVENT-SERVICE",
            "REQUEST-SERVICE",
            "COMMENT-SERVICE"
    );

    private static final Pattern UP_STATUS_PATTERN = Pattern.compile(
            "\"status\"\\s*:\\s*\"UP\"",
            Pattern.CASE_INSENSITIVE
    );

    private RequiredServicesStartupWaiter() {
    }

    public static void waitIfEnabled() {
        boolean enabled = Boolean.parseBoolean(
                System.getenv().getOrDefault(ENABLED_ENV, "false")
        );

        if (!enabled) {
            return;
        }

        String eurekaUrl = normalizeEurekaUrl(
                System.getenv().getOrDefault(
                        EUREKA_URL_ENV,
                        DEFAULT_EUREKA_URL
                )
        );

        int timeoutSeconds = readTimeoutSeconds();
        long deadline = System.nanoTime()
                + Duration.ofSeconds(timeoutSeconds).toNanos();

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(
                        REQUEST_TIMEOUT_SECONDS
                ))
                .build();

        while (System.nanoTime() < deadline) {
            List<String> missingServices = findMissingServices(
                    httpClient,
                    eurekaUrl
            );

            if (missingServices.isEmpty()) {
                System.out.println(
                        "All required services are registered in Eureka: "
                                + REQUIRED_SERVICES
                );
                return;
            }

            System.out.println(
                    "Gateway is waiting for Eureka services: "
                            + missingServices
            );

            sleepBeforeNextCheck();
        }

        throw new IllegalStateException(
                "Required services were not registered in Eureka within "
                        + timeoutSeconds
                        + " seconds: "
                        + REQUIRED_SERVICES
        );
    }

    private static List<String> findMissingServices(
            HttpClient httpClient,
            String eurekaUrl
    ) {
        List<String> missingServices = new ArrayList<>();

        for (String serviceName : REQUIRED_SERVICES) {
            if (!isServiceAvailable(
                    httpClient,
                    eurekaUrl,
                    serviceName
            )) {
                missingServices.add(serviceName);
            }
        }

        return missingServices;
    }

    private static boolean isServiceAvailable(
            HttpClient httpClient,
            String eurekaUrl,
            String serviceName
    ) {
        URI serviceUri = URI.create(
                eurekaUrl + "apps/" + serviceName
        );

        HttpRequest request = HttpRequest.newBuilder(serviceUri)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(
                        REQUEST_TIMEOUT_SECONDS
                ))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            return response.statusCode() == 200
                    && containsUpInstance(response.body());

        } catch (IOException exception) {
            return false;

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Waiting for Eureka services was interrupted",
                    exception
            );
        }
    }

    private static boolean containsUpInstance(String responseBody) {
        if (responseBody == null) {
            return false;
        }

        String normalizedBody = responseBody.toUpperCase(
                Locale.ROOT
        );

        return UP_STATUS_PATTERN.matcher(
                normalizedBody
        ).find();
    }

    private static String normalizeEurekaUrl(String eurekaUrl) {
        if (eurekaUrl.endsWith("/")) {
            return eurekaUrl;
        }

        return eurekaUrl + "/";
    }

    private static int readTimeoutSeconds() {
        String value = System.getenv(TIMEOUT_ENV);

        if (value == null || value.isBlank()) {
            return DEFAULT_TIMEOUT_SECONDS;
        }

        try {
            int timeoutSeconds = Integer.parseInt(value);

            if (timeoutSeconds <= 0) {
                throw new IllegalArgumentException(
                        TIMEOUT_ENV + " must be greater than zero"
                );
            }

            return timeoutSeconds;

        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    TIMEOUT_ENV + " must be an integer",
                    exception
            );
        }
    }

    private static void sleepBeforeNextCheck() {
        try {
            Thread.sleep(CHECK_INTERVAL_MILLIS);

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Waiting for Eureka services was interrupted",
                    exception
            );
        }
    }
}