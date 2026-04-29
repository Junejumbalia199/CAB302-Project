package kaggle;

/** Checked exception for any Kaggle API failure (auth, HTTP, parsing). */
public class KaggleException extends Exception {

    public KaggleException(String message) {
        super(message);
    }

    public KaggleException(String message, Throwable cause) {
        super(message, cause);
    }
}
