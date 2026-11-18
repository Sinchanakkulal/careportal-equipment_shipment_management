package common;

import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.Http;
import play.libs.Json;
import exceptions.ValidationException;

public final class JsonBody {
    
    private JsonBody(){}
    public static <T> T parse(Http.Request request, Class<T> type) {
        JsonNode json = request.body().asJson();

        if (json == null || !json.isObject()) {
            throw new ValidationException("The request body must be a JSON object.");
       
        }
        try {
            return Json.fromJson(json, type);
        } catch (RuntimeException e) {
            // never echo Jackson's message: it can reveal class names and internals
            throw new ValidationException("The request body contains invalid data.");
        }
    }
}
