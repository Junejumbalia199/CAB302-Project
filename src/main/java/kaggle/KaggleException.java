package kaggle;

/**
 * My own exception for anything that goes wrong talking to Kaggle — missing
 * creds, a non-2xx HTTP response, a response that didn't parse, etc.
 *
 * I made it checked on purpose. I want callers to think about what happens
 * when Kaggle is down or the user hasn't set up an API token yet, instead
 * of me silently swallowing it and leaving them wondering why the list is
 * empty.
 */
public class KaggleException extends Exception {
    public KaggleException(String message) {
        super(message);
    }

    public KaggleException(String message, Throwable cause) {
        super(message, cause);
    }
}
