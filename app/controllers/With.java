package controllers;

import actions.UserContextAction;

/**
 * With
 */
public @interface With {

    Class<UserContextAction> value();

}
