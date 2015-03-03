package ca.omny.ecs;

import ca.omny.extension.proxy.IRemoteUrlProvider;
import ca.omny.extension.proxy.IRemoteUrlProviderFactory;
import javax.inject.Inject;

public class EcsRemoteUrlProviderFactory implements IRemoteUrlProviderFactory {

    @Inject
    EcsTaskTracker taskTracker;
    
    @Inject
    EcsVersionMapper versionMapper;
    
    @Override
    public IRemoteUrlProvider getInstance(String name) {
        if(name.equals("ECS")) {
            return new EcsRemoteUrlProvider(taskTracker, versionMapper);
        }
        return null;
    }
    
}
