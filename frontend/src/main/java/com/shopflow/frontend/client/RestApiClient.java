package com.shopflow.frontend.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.frontend.client.dto.CartItemDto;
import com.shopflow.frontend.client.dto.CheckoutDtos;
import com.shopflow.frontend.client.dto.OrderDtos;
import com.shopflow.frontend.client.dto.ProductDto;
import com.shopflow.frontend.client.dto.SellerDtos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 백엔드 REST API 클라이언트(ADR-0011 BFF). 인증 전파는 서버 간 세션 쿠키 릴레이로 구현한다
 * (프론트가 사용자 대신 백엔드에 로그인하고, 받은 {@code JSESSIONID}·{@code XSRF-TOKEN} 쿠키를
 * 프론트 HttpSession({@link BackendSession})에 보관했다가 이후 호출마다 그대로 전달).
 */
@Component
public class RestApiClient {

    private static final Pattern COOKIE_VALUE = Pattern.compile("^([^;]+)");

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RestApiClient(@Value("${shopflow.backend.base-url:http://localhost:18000}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    // ===================== 인증 =====================

    /** 백엔드에 로그인해 세션·CSRF 쿠키를 받아온다. 실패 시 ApiException(401). */
    public BackendSession login(String email, String password) {
        MultiValueBody form = MultiValueBody.of("username", email, "password", password);
        var entity = restClient.post()
                .uri("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form.encoded())
                .retrieve()
                .onStatus(status -> status.value() == 401, (req, res) -> { /* 아래에서 401 처리 */ })
                .toEntity(String.class);
        if (entity.getStatusCode().value() == 401) {
            throw new ApiException(401, "이메일 또는 비밀번호가 올바르지 않습니다");
        }
        return extractSession(entity.getHeaders(), email);
    }

    private BackendSession extractSession(HttpHeaders headers, String email) {
        String jsession = null;
        String xsrf = null;
        for (String setCookie : headers.getOrDefault(HttpHeaders.SET_COOKIE, List.of())) {
            Matcher m = COOKIE_VALUE.matcher(setCookie);
            if (!m.find()) {
                continue;
            }
            String pair = m.group(1);
            if (pair.startsWith("JSESSIONID=")) {
                jsession = pair.substring("JSESSIONID=".length());
            } else if (pair.startsWith("XSRF-TOKEN=")) {
                xsrf = pair.substring("XSRF-TOKEN=".length());
            }
        }
        if (jsession == null) {
            throw new ApiException(401, "이메일 또는 비밀번호가 올바르지 않습니다");
        }
        return new BackendSession(jsession, xsrf, email, email);
    }

    public void logout(BackendSession session) {
        try {
            authed(session, restClient.post().uri("/logout")).retrieve().toBodilessEntity();
        } catch (RestClientResponseException ignored) {
            // 로그아웃 실패는 무시(세션은 어차피 프론트에서 폐기)
        }
    }

    public record SignupResult(Long userId, String email, String displayName) {
    }

    public SignupResult signup(String email, String password, String displayName) {
        Map<String, String> body = Map.of("email", email, "password", password, "displayName", displayName);
        return call(() -> restClient.post().uri("/api/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(SignupResult.class));
    }

    // ===================== 상품 =====================

    public List<ProductDto> browseProducts(String query) {
        String uri = UriComponentsBuilder.fromPath("/api/products")
                .queryParamIfPresent("q", java.util.Optional.ofNullable(query).filter(s -> !s.isBlank()))
                .build().toUriString();
        return call(() -> restClient.get().uri(uri).retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<ProductDto>>() {
                }));
    }

    public ProductDto getProduct(Long id) {
        return call(() -> restClient.get().uri("/api/products/{id}", id).retrieve().body(ProductDto.class));
    }

    public List<ProductDto> myProducts(BackendSession session) {
        return call(() -> authed(session, restClient.get().uri("/api/seller/products")).retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<ProductDto>>() {
                }));
    }

    public ProductDto createProduct(BackendSession session, String name, String description,
                                    long priceKrw, int stock, String imageUrl) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("description", description);
        body.put("priceKrw", priceKrw);
        body.put("stock", stock);
        body.put("imageUrl", imageUrl);
        return call(() -> authedWrite(session, restClient.post().uri("/api/seller/products"))
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(ProductDto.class));
    }

    public ProductDto updateProduct(BackendSession session, Long id, String name, String description,
                                    long priceKrw, String imageUrl) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("description", description);
        body.put("priceKrw", priceKrw);
        body.put("imageUrl", imageUrl);
        return call(() -> authedWrite(session, restClient.put().uri("/api/seller/products/{id}", id))
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(ProductDto.class));
    }

    public void changeStock(BackendSession session, Long id, int stock) {
        call(() -> authedWrite(session, restClient.put().uri("/api/seller/products/{id}/stock", id))
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("stock", stock)).retrieve().body(ProductDto.class));
    }

    public void changeStatus(BackendSession session, Long id, String status) {
        call(() -> authedWrite(session, restClient.post().uri("/api/seller/products/{id}/status", id))
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("status", status)).retrieve().body(ProductDto.class));
    }

    // ===================== 장바구니 =====================

    public List<CartItemDto> cartItems(BackendSession session) {
        return call(() -> authed(session, restClient.get().uri("/api/cart")).retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<CartItemDto>>() {
                }));
    }

    public void addCartItem(BackendSession session, Long productId, int quantity) {
        call(() -> authedWrite(session, restClient.post().uri("/api/cart/items"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("productId", productId, "quantity", quantity))
                .retrieve().body(CartItemDto.class));
    }

    public void updateCartItemQuantity(BackendSession session, Long itemId, int quantity) {
        call(() -> authedWrite(session, restClient.put().uri("/api/cart/items/{id}", itemId))
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("quantity", quantity))
                .retrieve().toBodilessEntity());
    }

    public void removeCartItem(BackendSession session, Long itemId) {
        call(() -> authedWrite(session, restClient.post().uri("/api/cart/items/{id}/delete", itemId))
                .retrieve().toBodilessEntity());
    }

    // ===================== 체크아웃 =====================

    public CheckoutDtos.Intent checkoutIntent(BackendSession session) {
        return call(() -> authed(session, restClient.get().uri("/api/checkout")).retrieve()
                .body(CheckoutDtos.Intent.class));
    }

    public CheckoutDtos.Result checkout(BackendSession session, String recipient, String address,
                                        String phone, String idempotencyKey) {
        Map<String, String> body = Map.of("recipient", recipient, "address", address,
                "phone", phone, "idempotencyKey", idempotencyKey);
        return call(() -> authedWrite(session, restClient.post().uri("/api/checkout"))
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(CheckoutDtos.Result.class));
    }

    // ===================== 주문 조회 =====================

    public List<OrderDtos.Summary> myOrders(BackendSession session) {
        return call(() -> authed(session, restClient.get().uri("/api/orders")).retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<OrderDtos.Summary>>() {
                }));
    }

    public OrderDtos.Detail orderDetail(BackendSession session, Long id) {
        return call(() -> authed(session, restClient.get().uri("/api/orders/{id}", id)).retrieve()
                .body(OrderDtos.Detail.class));
    }

    // ===================== 판매자 =====================

    public SellerDtos.SellerResponse registerSeller(BackendSession session, String sellerType, String storeName,
                                                     String brn, String repName, String phone, String email) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sellerType", sellerType);
        body.put("storeName", storeName);
        body.put("businessRegistrationNumber", brn);
        body.put("representativeName", repName);
        body.put("contactPhone", phone);
        body.put("contactEmail", email);
        return call(() -> authedWrite(session, restClient.post().uri("/api/seller"))
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(SellerDtos.SellerResponse.class));
    }

    public List<SellerDtos.SellerSubOrderView> sellerOrders(BackendSession session) {
        return call(() -> authed(session, restClient.get().uri("/api/seller/orders")).retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<SellerDtos.SellerSubOrderView>>() {
                }));
    }

    public void ship(BackendSession session, Long subOrderId) {
        call(() -> authedWrite(session, restClient.post().uri("/api/seller/orders/{id}/ship", subOrderId))
                .retrieve().body(SellerDtos.DeliveryView.class));
    }

    public void deliver(BackendSession session, Long subOrderId) {
        call(() -> authedWrite(session, restClient.post().uri("/api/seller/orders/{id}/deliver", subOrderId))
                .retrieve().body(SellerDtos.DeliveryView.class));
    }

    // ===================== 내부 =====================

    private RestClient.RequestHeadersSpec<?> authed(BackendSession session, RestClient.RequestHeadersSpec<?> spec) {
        return spec.header(HttpHeaders.COOKIE, cookieHeader(session));
    }

    /** 상태변경 요청은 쿠키 외에 CSRF 헤더도 함께 실어 보낸다. */
    private RestClient.RequestBodySpec authedWrite(BackendSession session, RestClient.RequestBodySpec spec) {
        spec.header(HttpHeaders.COOKIE, cookieHeader(session));
        if (session.xsrfToken() != null) {
            spec.header("X-XSRF-TOKEN", session.xsrfToken());
        }
        return spec;
    }

    private String cookieHeader(BackendSession session) {
        StringBuilder sb = new StringBuilder("JSESSIONID=").append(session.jsessionId());
        if (session.xsrfToken() != null) {
            sb.append("; XSRF-TOKEN=").append(session.xsrfToken());
        }
        return sb.toString();
    }

    private <T> T call(java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (RestClientResponseException e) {
            throw new ApiException(e.getStatusCode().value(), extractDetail(e));
        }
    }

    private String extractDetail(RestClientResponseException e) {
        try {
            JsonNode node = objectMapper.readTree(e.getResponseBodyAsString());
            if (node.has("detail")) {
                return node.get("detail").asText();
            }
        } catch (Exception ignored) {
            // 파싱 실패 시 기본 메시지로 폴백
        }
        return "요청을 처리할 수 없습니다";
    }

    private record MultiValueBody(String... kv) {
        static MultiValueBody of(String... kv) {
            return new MultiValueBody(kv);
        }

        String encoded() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < kv.length; i += 2) {
                if (i > 0) {
                    sb.append('&');
                }
                sb.append(java.net.URLEncoder.encode(kv[i], java.nio.charset.StandardCharsets.UTF_8))
                        .append('=')
                        .append(java.net.URLEncoder.encode(kv[i + 1], java.nio.charset.StandardCharsets.UTF_8));
            }
            return sb.toString();
        }
    }
}
