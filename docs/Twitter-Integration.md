# Twitter OSINT Integration

## Overview

The backend now exposes first-party REST endpoints that wrap the Twitter (X) API v2 for OSINT scenarios such as user profiling, keyword monitoring, and hashtag tracking. Requests are proxied through `TwitterService`, which manages OAuth2 application-only authentication, retries under rate limiting, and normalises response payloads for the CyberScope dashboard.

## Configuration

Set the following environment variables (or update `.env`) before starting the Spring Boot backend:

- `TWITTER_API_KEY` – consumer API key (aka `api_key`).
- `TWITTER_API_SECRET` – consumer API secret.
- `TWITTER_BEARER_TOKEN` *(optional)* – pre-generated bearer token to skip runtime negotiation. Leave empty to let the service request a token automatically.


> **Note:** The free tier is limited to roughly 1,500 tweets per month. The service automatically re-fetches a bearer token if Twitter invalidates the cached one, but it does not currently handle usage quota exhaustion—surface the `error` payload to end users so they can request upgraded access.

## REST Endpoints

All endpoints live under `/api/twitter` and return JSON payloads.

### `GET /api/twitter/tweets/search`

- Query parameters:
  - `query` *(required)* – free-text search (Twitter advanced syntax supported).
  - `hashtag` *(optional)* – appended to the query (auto-prefixes `#`).
  - `location` *(optional)* – raw fragment appended to the query (use `point_radius`, `place`, etc.).
  - `maxResults` *(optional, default 25)* – clamped to Twitter’s 10–100 window.
- Response includes `tweets`, `meta`, and optional `includes.users` collections aligned with the Twitter API.

## Error Handling

- Service-level issues (missing credentials, upstream failures) respond with an `error` field and an HTTP status mapped from Twitter’s response.
- 401 responses automatically clear the cached bearer token and force a re-authentication on the next request.

## Scan Workflow Usage

- Select the `Social Media Monitor` scan type and include **Twitter** in the providers list.
- Target formats accepted by the scan engine (all map to a `/2/tweets/search/recent` query):
  - `@username` → treated as a raw search string (use `from:username` for author filtering).
  - `#hashtag` → searches tweets containing the hashtag.
  - `keyword` or `keyword one keyword two` → executes a keyword search across recent tweets.
  - `keyword | location qualifier` → appends a location fragment (e.g., `phishing kit | point_radius:[41.01,28.97,20km]`).
- Results are stored alongside other provider outputs and can feed existing Gemini analysis pipelines.

## Next Steps

- Surface these endpoints through the frontend dashboard (e.g., new widgets for phishing keyword monitoring).
- Persist results or run scheduled scans to catch emerging phishing campaigns in near real-time.

