package osint.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import reactor.netty.http.client.HttpClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

@Service
public class TwitterService {

    private static final Logger logger = LoggerFactory.getLogger(TwitterService.class);

    private static final String[] DEFAULT_TWEET_FIELDS = new String[]{
            "id",
            "text",
            "created_at",
            "lang",
            "possibly_sensitive",
            "public_metrics",
            "source",
            "author_id"
    };

    private static final String[] DEFAULT_EXPANSIONS = new String[]{
            "author_id"
    };

    private static final String[] DEFAULT_USER_FIELDS = new String[]{
            "username",
            "name",
            "profile_image_url",
            "verified",
            "public_metrics",
            "created_at",
            "description",
            "location",
            "url"
    };

    private final WebClient apiClient;
    private final WebClient authClient;
    private final String apiKey;
    private final String apiSecret;
    private final AtomicReference<String> cachedBearerToken = new AtomicReference<>();

    public TwitterService(
            @Value("${osint.twitter.api-key:}") String apiKey,
            @Value("${osint.twitter.api-secret:}") String apiSecret,
            @Value("${osint.twitter.bearer-token:}") String initialBearerToken) {

        this.apiKey = apiKey;
        this.apiSecret = apiSecret;

        if (StringUtils.hasText(initialBearerToken)) {
            cachedBearerToken.set(initialBearerToken);
        }

        HttpClient apiHttpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(30));
        HttpClient authHttpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(10));

        this.apiClient = WebClient.builder()
                .baseUrl("https://api.twitter.com/2")
                .clientConnector(new ReactorClientHttpConnector(apiHttpClient))
                .build();

        this.authClient = WebClient.builder()
                .baseUrl("https://api.twitter.com")
                .defaultHeaders(headers -> headers.setBasicAuth(this.apiKey, this.apiSecret))
                .clientConnector(new ReactorClientHttpConnector(authHttpClient))
                .build();
    }

    public Mono<Map<String, Object>> getUserProfile(String username) {
        if (!isConfigured()) {
            return Mono.just(createError("Twitter API credentials not configured", 500));
        }

        return getBearerToken()
                .flatMap(token -> apiClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/users/by/username/{username}")
                                .queryParam("user.fields", String.join(",", DEFAULT_USER_FIELDS))
                                .build(username))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .onStatus(status -> status.isError(), response -> response.bodyToMono(Map.class)
                                .defaultIfEmpty(Map.of())
                                .flatMap(body -> {
                                    if (response.statusCode().value() == 401) {
                                        invalidateBearerToken();
                                    }
                                    logger.error("Twitter profile request failed: status={}, body={}", response.statusCode(), body);
                                    return Mono.error(new TwitterServiceException("profile_request_failed", response.statusCode().value(), body));
                                }))
                        .bodyToMono(Map.class))
                .map(this::wrapDataOrError);
    }

    public Mono<Map<String, Object>> searchRecentTweets(String query, String hashtag, String location, int maxResults) {
        if (!isConfigured()) {
            return Mono.just(createError("Twitter API credentials not configured", 500));
        }

        return getBearerToken()
                .flatMap(token -> apiClient.get()
                        .uri(uriBuilder -> buildSearchUri(uriBuilder, buildQuery(query, hashtag, location), maxResults))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .onStatus(status -> status.isError(), response -> response.bodyToMono(Map.class)
                                .defaultIfEmpty(Map.of())
                                .flatMap(body -> {
                                    if (response.statusCode().value() == 401) {
                                        invalidateBearerToken();
                                    }
                                    logger.error("Twitter search request failed: status={}, body={}", response.statusCode(), body);
                                    return Mono.error(new TwitterServiceException("tweet_search_failed", response.statusCode().value(), body));
                                }))
                        .bodyToMono(Map.class))
                .map(this::wrapDataOrError);
    }

    public Mono<Map<String, Object>> searchHashtag(String hashtag, int maxResults) {
        String normalized = hashtag.startsWith("#") ? hashtag : "#" + hashtag;
        return searchRecentTweets(normalized, null, null, maxResults);
    }

    private Mono<String> getBearerToken() {
        String existing = cachedBearerToken.get();
        if (StringUtils.hasText(existing)) {
            return Mono.just(existing);
        }

        if (!isConfigured()) {
            return Mono.error(new IllegalStateException("Twitter API credentials are not configured"));
        }

        return authClient.post()
                .uri("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "client_credentials"))
                .retrieve()
                .onStatus(status -> status.isError(), response -> response.bodyToMono(Map.class)
                        .defaultIfEmpty(Map.of())
                        .flatMap(body -> {
                            logger.error("Twitter bearer token request failed: status={}, body={}", response.statusCode(), body);
                            return Mono.error(new TwitterServiceException("bearer_request_failed", response.statusCode().value(), body));
                        }))
                .bodyToMono(Map.class)
                .map(tokenResponse -> Objects.toString(tokenResponse.get("access_token"), ""))
                .flatMap(token -> {
                    if (!StringUtils.hasText(token)) {
                        return Mono.error(new TwitterServiceException("missing_access_token", 500, Map.of("error", "Missing access_token in response")));
                    }
                    cachedBearerToken.set(token);
                    logger.info("Obtained new Twitter bearer token");
                    return Mono.just(token);
                });
    }

    private void invalidateBearerToken() {
        cachedBearerToken.set(null);
    }

    private boolean isConfigured() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(apiSecret);
    }

    private Map<String, Object> wrapDataOrError(Map<String, Object> response) {
        if (response == null) {
            return createError("Empty response from Twitter API", 502);
        }
        if (response.containsKey("errors")) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "twitter_api_error");
            error.put("details", response.get("errors"));
            error.put("status", 502);
            return error;
        }
        return response;
    }

    private String buildQuery(String query, String hashtag, String location) {
        StringBuilder builder = new StringBuilder();

        if (StringUtils.hasText(query)) {
            builder.append(query.trim());
        }

        if (StringUtils.hasText(hashtag)) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            String normalized = hashtag.trim();
            if (!normalized.startsWith("#")) {
                normalized = "#" + normalized;
            }
            builder.append(normalized);
        }

        if (StringUtils.hasText(location)) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(location.trim());
        }

        return builder.toString().trim();
    }

    private URI buildSearchUri(UriBuilder builder, String query, int maxResults) {
        if (!StringUtils.hasText(query)) {
            throw new TwitterServiceException("invalid_query", 400, Map.of("error", "Query must not be empty"));
        }

        return builder
                .path("/tweets/search/recent")
                .queryParam("query", query)
                .queryParam("max_results", Math.min(Math.max(maxResults, 10), 100))
                .queryParam("tweet.fields", String.join(",", DEFAULT_TWEET_FIELDS))
                .queryParam("expansions", String.join(",", DEFAULT_EXPANSIONS))
                .queryParam("user.fields", String.join(",", DEFAULT_USER_FIELDS))
                .build();
    }

    private Map<String, Object> createError(String message, int status) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("status", status);
        return error;
    }

    public static class TwitterServiceException extends RuntimeException {
        private final String errorCode;
        private final int statusCode;
        private final Map<String, Object> body;

        public TwitterServiceException(String errorCode, int statusCode, Map<String, Object> body) {
            super(errorCode);
            this.errorCode = errorCode;
            this.statusCode = statusCode;
            this.body = body;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public Map<String, Object> getBody() {
            return body;
        }
    }
}

