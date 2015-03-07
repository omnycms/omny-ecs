package ca.omny.ecs;

import ca.omny.extension.proxy.IRemoteUrlProvider;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

@Alternative
public class EcsRemoteUrlProvider implements IRemoteUrlProvider {

    @Inject
    EcsTaskTracker taskTracker;
    
    @Inject
    EcsVersionMapper mapper;

    public EcsRemoteUrlProvider(EcsTaskTracker taskTracker, EcsVersionMapper mapper) {
        this.taskTracker = taskTracker;
        this.mapper = mapper;
    }
    
    @Override
    public String getRemoteUrl(String route, HttpServletRequest req) {
        String cluster = System.getenv("OMNY_ECS_CLUSTER");
        String[] routeParts = route.split("/");
        String api = "";
        if(routeParts.length>0) {
            api = routeParts[0];
            if(routeParts[1].equals("api")) {
                api = routeParts[3];
            }
        }
        String family = mapper.getFamily(api);
        String version = mapper.getCurrentVersion(family);
        Map<String, List<Integer>> hostPortMapping = taskTracker.getHostPortMapping(family, version);
        Set<String> keySet = hostPortMapping.keySet();
        Random r = new Random();
        int pos = r.nextInt(keySet.size());
        int count =0;
        for(String key: keySet) {
            if(count==pos) {
                List<Integer> ports = hostPortMapping.get(key);
                int port = ports.get(r.nextInt(ports.size()));
                return "http://"+key+":"+port+route+"?"+req.getQueryString();
            }
            count++;
        }
        return null;
    }

    @Override
    public String getId() {
        return "ECS";
    }
    
}
