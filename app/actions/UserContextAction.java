package actions;

import java.util.concurrent.CompletionStage;
import javax.inject.Inject;

import com.typesafe.config.Config;
import play.libs.typedmap.TypedKey;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

/** Attaches the (training) user ID to every request that uses @With(UserContextAction.class) */
public class UserContextAction extends Action.Simple {

    /** Key used by controllers to read the user ID from the request */
    public static final TypedKey<String> USER_ID = TypedKey.create("userId");

    private final String userId;

    @Inject
    public UserContextAction(Config config) {
        this.userId = config.getString("careportal.userId");
    }

    @Override
    public CompletionStage<Result> call(Http.Request request) {
        return delegate.call(request.addAttr(USER_ID, userId));
    }

    public class USER_ID {
    }
}