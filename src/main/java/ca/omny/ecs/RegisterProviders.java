package ca.omny.ecs;

import ca.omny.ecs.lambda.LambdaProxy;
import ca.omny.potent.PowerServlet;
import ca.omny.potent.RemoteUrlProviderFactory;

public class RegisterProviders {
    static {
        RemoteUrlProviderFactory.addRemoteUrlProviderFactory(new EcsRemoteUrlProviderFactory());
        PowerServlet.addProxyService(new LambdaProxy());
    }
}
