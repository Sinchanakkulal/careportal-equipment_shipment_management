package controllers;

import play.mvc.Controller;
import play.mvc.Result;

public class HomeController extends Controller {

    /** GET /  ->  the frontend home page */
    public Result index() {
        return redirect("/assets/index.html");
    }
}   