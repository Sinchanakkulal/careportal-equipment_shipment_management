package exceptions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Singleton;

import common.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.http.HttpErrorHandler;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;

/**
 * The single place where exceptions become HTTP responses.
 * Controllers and services never build error responses themselves.
 */
@Singleton
public class GlobalErrorHandler implements HttpErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);
    public static final String MSG_SERVER_ERROR =
            "Something went wrong on the server. Please try again later.";

    /**
     * 4xx problems Play detects itself: unknown URL (404), wrong method (405),
     * unreadable JSON body (400)...
     * The incoming "message" is NOT passed on, because it can contain internal details.
     */
    @Override
    public CompletionStage<Result> onClientError(RequestHeader request, int statusCode, String message) {
        log.info("{} {} -> client error {}", request.method(), request.uri(), statusCode);
        return respond(statusCode, clientMessage(statusCode));

    }

    /** Anything thrown while handling a request */
    @Override
    public CompletionStage<Result> onServerError(RequestHeader request, Throwable exception) {

        // Play wraps exceptions, so look through the cause chain for ours
        Throwable known = findBusinessException(exception);

        if (known != null) {
            int status = statusFor(known);
            log.info("{} {} -> {} {}", request.method(), request.uri(), status, known.getMessage());
            return respond(status, known.getMessage());
        }

        // Unexpected failure (SQL error, bug...): full details to the LOG only
        log.error("Unhandled error on {} {}", request.method(), request.uri(), exception);
        return respond(500,MSG_SERVER_ERROR);
    }

    /* ---------- helpers ---------- */

    private static CompletionStage<Result> respond(int status, String message) {
        return CompletableFuture.completedFuture(ApiResponses.error(status, message));
    }

    /** Walks down the cause chain and returns the first exception we know how to map */
    private static Throwable findBusinessException(Throwable throwable) {
        Throwable current = throwable;
        for (int depth = 0; current != null && depth < 10; depth++) {
            if (current instanceof ValidationException
                    || current instanceof ResourceNotFoundException

                    || current instanceof DuplicateResourceException
                    || current instanceof InvalidStatusTransitionException) {
                return current;
            }
            current = current.getCause();
        }
        return null;
    }

    private static int statusFor(Throwable throwable) {
        if (throwable instanceof ValidationException)             return 400;
        if (throwable instanceof ResourceNotFoundException)       return 404;
        if (throwable instanceof DuplicateResourceException)      return 409;
        if (throwable instanceof InvalidStatusTransitionException) return 409;
        return 500;
    }

    private static String clientMessage(int statusCode) {
        switch (statusCode) {
            case 400: return "The request could not be understood. Please check the data and try again.";
            case 404: return "The requested resource was not found.";
            case 405: return "This operation is not allowed for this URL.";
            case 413: return "The request is too large.";
            case 415: return "Unsupported content type. Send JSON.";
            default:  return "The request could not be processed.";
        }
    }
}