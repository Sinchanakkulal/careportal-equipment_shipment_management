package controllers;

import javax.inject.Inject;

import actions.UserContextAction;
import common.ApiResponses;
import common.JsonBody;
import common.QueryParams;
import dto.EquipmentRequest;
import dto.EquipmentResponse;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;
import services.EquipmentService;
import dto.StatusChangeRequest;
import play.libs.Json;

public class EquipmentController extends Controller {

    private final EquipmentService equipmentService;

    @Inject
    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    /** POST /api/equipment  ->  201 + the created record */
    @With(UserContextAction.class)
    public Result create(Http.Request request) {
        EquipmentRequest body = JsonBody.parse(request, EquipmentRequest.class);
        String userId = request.attrs().get(UserContextAction.USER_ID);

        EquipmentResponse created = equipmentService.create(body, userId);
        return ApiResponses.created(created);
    }

    /** STEP 4: GET /api/equipment?search=&status=&type=&page=&size=  ->  200 + PagedResponse */
    public Result list() {
        return ok(Json.toJson(equipmentService.list()));
    }

    /** STEP 4: GET /api/equipment/:id  ->  200 + the record, or 404 */
    public Result get(Long id) {
        return ApiResponses.ok(equipmentService.getById(id));
    }

    /** STEP 5: PUT /api/equipment/:id  ->  200 + the updated record */
    @With(UserContextAction.class)
    public Result update(Http.Request request, Long id) {
        EquipmentRequest body = JsonBody.parse(request, EquipmentRequest.class);
        String userId = request.attrs().get(UserContextAction.USER_ID);

        return ApiResponses.ok(equipmentService.update(id, body, userId));
    }

     /** STEP 6: PATCH /api/equipment/:id/status  body {"status":"Approved"}  ->  200 + the updated record */
    @With(UserContextAction.class)
    public Result changeStatus(Http.Request request, Long id) {
        StatusChangeRequest body = JsonBody.parse(request, StatusChangeRequest.class);
        String userId = request.attrs().get(UserContextAction.USER_ID);

        return ApiResponses.ok(equipmentService.changeStatus(id, body, userId));
    }
}