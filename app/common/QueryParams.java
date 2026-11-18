package common;

import exceptions.ValidationException;
import play.mvc.Http;

public final class QueryParams {

    private QueryParams(){}

    public static int intValue(Http.Request request, String name, int defaultValue) {
        String raw = request.getQueryString(name);
        if (raw == null || raw.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(name + " must be a whole number.");
        }
    }
      
        /** Missing or blank -> null. Not a whole number -> 400. */
    public static Long optionalLong(Http.Request request, String name) {
        String raw = request.getQueryString(name);
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(name + " must be a whole number.");
        }
    }
}
