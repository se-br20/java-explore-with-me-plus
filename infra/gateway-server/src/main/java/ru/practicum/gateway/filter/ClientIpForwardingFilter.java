package ru.practicum.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Slf4j
@Component
public class ClientIpForwardingFilter implements GlobalFilter, Ordered {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";
    private static final String X_CLIENT_IP = "X-Client-IP";

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {
        String clientIp = resolveClientIp(exchange);

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> {
                    headers.remove(X_CLIENT_IP);
                    headers.add(X_CLIENT_IP, clientIp);
                })
                .build();

        log.debug(
                "Forwarding request: path={}, clientIp={}",
                request.getURI().getPath(),
                clientIp
        );

        return chain.filter(
                exchange.mutate()
                        .request(request)
                        .build()
        );
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest()
                .getHeaders()
                .getFirst(X_FORWARDED_FOR);

        String forwardedIp = extractFirstAddress(forwardedFor);

        if (forwardedIp != null) {
            return forwardedIp;
        }

        String realIpHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(X_REAL_IP);

        String realIp = extractFirstAddress(realIpHeader);

        if (realIp != null) {
            return realIp;
        }

        InetSocketAddress remoteAddress =
                exchange.getRequest().getRemoteAddress();

        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return "unknown";
    }

    private String extractFirstAddress(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }

        String firstAddress = headerValue.split(",")[0].trim();

        if (firstAddress.isBlank()
                || "unknown".equalsIgnoreCase(firstAddress)) {
            return null;
        }

        return firstAddress;
    }

    @Override
    public int getOrder() {

        return Ordered.HIGHEST_PRECEDENCE;
    }
}