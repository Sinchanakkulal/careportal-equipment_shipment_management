package exceptions;

/** Mandatory field missing, wrong type, too long, bad date, etc.  ->  HTTP 400 */
public class ValidationException extends RuntimeException{
    public ValidationException(String message){
        super(message);
    }
}
