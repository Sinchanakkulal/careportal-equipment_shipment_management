package controllers;

import javax.inject.Inject;

import actions.UserContextAction;
import common.ApiResponses;
import common.JsonBody;
import dto.ShipmentRequest;
import dto.StatusChangeRequest;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;
import services.ShipmentService;

public class ShipmentController extends Controller {

    private final ShipmentService shipmentService;

    @Inject
    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    /** POST /api/shipments  ->  201 + the created shipment */
    @With(UserContextAction.class)
    public Result create(Http.Request request) {
        ShipmentRequest body = JsonBody.parse(request, ShipmentRequest.class);
        String userId = request.attrs().get(UserContextAction.USER_ID);

        return ApiResponses.created(shipmentService.create(body, userId));
    }

    public Result list() {
        return ApiResponses.ok(shipmentService.list());
    }

    /** GET /api/shipments/:id  ->  200 + the shipment, or 404 */
    public Result get(Long id) {
        return ApiResponses.ok(shipmentService.getById(id));
    }

    /** PUT /api/shipments/:id  ->  200 + the updated shipment */
    @With(UserContextAction.class)
    public Result update(Http.Request request, Long id) {
        ShipmentRequest body = JsonBody.parse(request, ShipmentRequest.class);
        String userId = request.attrs().get(UserContextAction.USER_ID);

        return ApiResponses.ok(shipmentService.update(id, body, userId));
    }

    /** PATCH /api/shipments/:id/status  body {"status":"Delivered","actualDeliveryDate":"2026-11-05"} */
    @With(UserContextAction.class)
    public Result changeStatus(Http.Request request, Long id) {
        StatusChangeRequest body = JsonBody.parse(request, StatusChangeRequest.class);
        String userId = request.attrs().get(UserContextAction.USER_ID);

        return ApiResponses.ok(shipmentService.changeStatus(id, body, userId));
    }
}