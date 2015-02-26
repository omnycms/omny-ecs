package ca.omny.ecs;

import ca.omny.extension.proxy.IRemoteUrlProvider;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

public class MainTest {
    public static void main(String[] args) {
        Weld weld = new Weld();
        WeldContainer container = weld.initialize();
        /*EcsTaskTracker taskTracker = container.instance().select(EcsTaskTracker.class).get();
        Map<String, List<Integer>> hostPortMapping = taskTracker.getHostPortMapping("omny-proxy", "7");
        for(String key: hostPortMapping.keySet()) {
            System.out.println(key+":"+hostPortMapping.get(key));
        }*/
        
        IRemoteUrlProvider provider = container.instance().select(IRemoteUrlProvider.class).get();
        for(int i=0; i<10; i++) {
            long startTime = System.nanoTime();
            System.out.println(provider.getRemoteUrl("/api/x/omny-proxy", null));
            long endTime = System.nanoTime();

            long duration = (endTime - startTime);
            System.out.println(duration);
        }
    }
}
