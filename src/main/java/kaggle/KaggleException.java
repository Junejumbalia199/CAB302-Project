package kaggle;

/** Checked exception for any Kaggle API failure (auth, HTTP, parsing). */
public class KaggleException extends Exception {

    /**
     * Constructs a new KaggleException with the given detail message.
     * @param message description of the failure
     */
    public KaggleException(String message) {
        super(message);
    }

    /**
     * Constructs a new KaggleException with a detail message and a cause.
     * @param message description of the failure
     * @param cause   the underlying exception that triggered this one
     */
    public KaggleException(String message, Throwable cause) {
        super(message, cause);
    }
}
