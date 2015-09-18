package ca.omny.ecs;

import ca.omny.extension.proxy.IRemoteUrlProvider;
import ca.omny.extension.proxy.IRemoteUrlProviderFactory;

public class EcsRemoteUrlProviderFactory implements IRemoteUrlProviderFactory {

    EcsTaskTracker taskTracker;
    
    EcsVersionMapper versionMapper;
    
    @Override
    public IRemoteUrlProvider getInstance(String name) {
        if(name.equals("ECS")) {
            return new EcsRemoteUrlProvider(new EcsTaskTracker(), new EcsVersionMapper());
        }
        return null;
    }
    
}
