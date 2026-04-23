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
 * My little wrapper around Kaggle's REST API (https://www.kaggle.com/api/v1).
 *
 * Kaggle doesn't ship an official Java SDK so I just hit the endpoints
 * directly. I only exposed the two things my app actually uses: searching
 * for datasets, and grabbing a single file out of one. Everything else I
 * can add later if I need it.
 *
 * The responses come back as raw JSON strings. I didn't want to bake a
 * specific shape into this class, so the bit of code that actually knows
 * what it wants (ExerciseRepository, in our case) does the parsing.
 *
 * Auth is HTTP Basic — username:key base64-encoded. I set the HttpClient
 * to follow redirects automatically because dataset downloads 302 over to
 * storage.googleapis.com and I don't want to babysit that dance.
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

    /**
     * Fires a search at /datasets/list?search=... and hands back the raw
     * JSON body. If anything goes wrong I wrap it in a KaggleException with
     * the HTTP status so the caller can decide whether to retry or just
     * fall back to the hardcoded list.
     */
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

    /**
     * Grabs one specific file from a dataset instead of the whole zip
     * archive. I use this so I don't have to unzip a 20 MB bundle just to
     * read one CSV. Kaggle 302s over to storage.googleapis.com and the
     * HttpClient follows the redirect for me.
     *
     * @return the path I was handed (for chaining, nothing clever).
     */
    public Path downloadFile(String owner, String dataset, String fileName, Path target)
            throws KaggleException {
        // I need to create the parent directory myself. If I don't,
        // BodyHandlers.ofFile throws a NoSuchFileException that points at
        // the *parent* and it's confusing to debug.
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
            // If the request failed I don't want a half-written file sitting
            // around — next run would see it and skip the download. So I
            // delete it. Best-effort; if the delete itself fails there's
            // nothing sensible to do about it.
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
