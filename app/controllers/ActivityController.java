package controllers;

import javax.inject.Inject;

import common.ApiResponses;
import play.mvc.Controller;
import play.mvc.Result;
import services.ActivityService;

/** Read-only. There is deliberately no create, update or delete action here. */
public class ActivityController extends Controller {

    private final ActivityService activityService;

    @Inject
    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    /** GET /api/activity?entityType=&entityId=&page=&size=  ->  200 + PagedResponse, newest first */
    public Result list() {

        return ApiResponses.ok(activityService.list());
    }
}