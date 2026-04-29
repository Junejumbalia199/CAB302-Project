package kaggle;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;

/**
 * Thin Kaggle REST API client (https://www.kaggle.com/api/v1).
 * HTTP Basic auth, follows 302 redirects to googleapis storage. Returns raw JSON
 * for callers to parse.
 */
public final class KaggleClient {

    private static final String BASE = "https://www.kaggle.com/api/v1";

    private final HttpClient http;
    private final String     authHeader;

    public KaggleClient(KaggleConfig cfg) {
        this.http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        String raw = cfg.username() + ":" + cfg.key();
        this.authHeader = "Basic " + Base64.getEncoder()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    // ── search ────────────────────────────────────────────────────────────────

    /** GET /datasets/list?search={query}. Returns raw JSON body. */
    public String searchDatasets(String query) throws KaggleException {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        HttpRequest req = baseRequest(BASE + "/datasets/list?search=" + encoded)
                .GET()
                .build();

        HttpResponse<String> res = send(req, HttpResponse.BodyHandlers.ofString(),
                "search failed for \"" + query + "\"");
        if (res.statusCode() != 200) {
            throw new KaggleException(
                    "Kaggle search returned HTTP " + res.statusCode() +
                    " — body: " + truncate(res.body(), 200));
        }
        return res.body();
    }

    // ── download ──────────────────────────────────────────────────────────────

    /** Downloads single file from dataset to target path. Returns target. */
    public Path downloadFile(String owner, String dataset, String fileName, Path target)
            throws KaggleException {
        // Create parent dir; ofFile fails opaquely otherwise.
        try {
            Path parent = target.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
        } catch (IOException e) {
            throw new KaggleException(
                    "Could not prepare download target " + target + ": " + e.getMessage(), e);
        }

        String url = BASE + "/datasets/download/" + owner + "/" + dataset + "/" + fileName;
        HttpRequest req = baseRequest(url).GET().build();

        HttpResponse<Path> res = send(req, HttpResponse.BodyHandlers.ofFile(target),
                "download failed for " + owner + "/" + dataset + "/" + fileName);
        if (res.statusCode() != 200) {
            // Drop partial file so next run retries instead of skipping.
            try { Files.deleteIfExists(target); } catch (IOException ignored) { }
            throw new KaggleException(
                    "Kaggle download returned HTTP " + res.statusCode() +
                    " for " + owner + "/" + dataset + "/" + fileName);
        }
        return res.body();
    }

    // ── internals ─────────────────────────────────────────────────────────────

    private HttpRequest.Builder baseRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", authHeader)
                .header("User-Agent", "CAB302-Project/1.0 (Java HttpClient)")
                .timeout(Duration.ofSeconds(60));
    }

    private <T> HttpResponse<T> send(HttpRequest req,
                                     HttpResponse.BodyHandler<T> handler,
                                     String errorContext) throws KaggleException {
        try {
            return http.send(req, handler);
        } catch (IOException e) {
            throw new KaggleException(errorContext + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KaggleException(errorContext + " was interrupted", e);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
