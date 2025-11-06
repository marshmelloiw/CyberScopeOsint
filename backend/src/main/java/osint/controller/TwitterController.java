package osint.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import osint.service.TwitterService;
import osint.service.TwitterService.TwitterServiceException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/twitter")
public class TwitterController {

    private final TwitterService twitterService;

    @Autowired
    public TwitterController(TwitterService twitterService) {
        this.twitterService = twitterService;
    }

    @GetMapping("/profile/{username}")
    public Mono<ResponseEntity<Map<String, Object>>> getUserProfile(@PathVariable String username) {
        return twitterService.getUserProfile(username)
                .map(this::toResponseEntity);
    }

    @GetMapping("/tweets/search")
    public Mono<ResponseEntity<Map<String, Object>>> searchTweets(
            @RequestParam String query,
            @RequestParam(required = false) String hashtag,
            @RequestParam(required = false) String location,
            @RequestParam(name = "maxResults", defaultValue = "25") int maxResults) {
        return twitterService.searchRecentTweets(query, hashtag, location, maxResults)
                .map(this::toResponseEntity);
    }

    @GetMapping("/hashtags/{hashtag}")
    public Mono<ResponseEntity<Map<String, Object>>> searchHashtag(
            @PathVariable String hashtag,
            @RequestParam(name = "maxResults", defaultValue = "25") int maxResults) {
        return twitterService.searchHashtag(hashtag, maxResults)
                .map(this::toResponseEntity);
    }

    @ExceptionHandler(TwitterServiceException.class)
    public ResponseEntity<Map<String, Object>> handleTwitterServiceException(TwitterServiceException ex) {
        Map<String, Object> body = ex.getBody() == null ? Map.of("error", ex.getErrorCode()) : ex.getBody();
        int status = ex.getStatusCode() > 0 ? ex.getStatusCode() : HttpStatus.BAD_GATEWAY.value();
        return ResponseEntity.status(status).body(body);
    }

    private ResponseEntity<Map<String, Object>> toResponseEntity(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "empty_response", "status", HttpStatus.BAD_GATEWAY.value()));
        }

        Object statusValue = body.get("status");
        if (statusValue instanceof Number) {
            int status = ((Number) statusValue).intValue();
            HttpStatus httpStatus = HttpStatus.resolve(status);
            if (httpStatus == null) {
                httpStatus = HttpStatus.BAD_GATEWAY;
            }
            return ResponseEntity.status(httpStatus).body(body);
        }

        if (body.containsKey("error")) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
        }

        return ResponseEntity.ok(body);
    }
}

