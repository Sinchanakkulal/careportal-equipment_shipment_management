package common;

import dto.ErrorResponse;
import play.libs.Json;
import play.mvc.Results;

public final class ApiResponses {

    private ApiResponses() {
        // Prevent instantiation
    }

    /** 200 with a JSON body */
    public static play.mvc.Result ok(Object body) {
        return Results.ok(Json.toJson(body));
    }

    /** 201 with a JSON body */
    public static play.mvc.Result created(Object body) {
        return Results.created(Json.toJson(body));
    }

    /** Any error status with the standard {"error": "..."} body */
    public static play.mvc.Result error(int status, String message) {
        return Results.status(status, Json.toJson(new ErrorResponse(message)));
    }
}
