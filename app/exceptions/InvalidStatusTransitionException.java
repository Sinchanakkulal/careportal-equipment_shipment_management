package exceptions;

/** The requested status change breaks the workflow rules.  ->  HTTP 409 */
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}