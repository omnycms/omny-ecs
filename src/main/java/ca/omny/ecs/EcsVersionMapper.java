package ca.omny.ecs;

import ca.omny.documentdb.IDocumentQuerier;
import java.util.HashMap;
import java.util.Map;

public class EcsVersionMapper {
    
    Map<String,String> familyMap = new HashMap<>();
    
    public String getCurrentVersion(String family, IDocumentQuerier querier) {
        String key = querier.getKey("services",family,"current");
        return querier.get(key, Map.class).get("version").toString();
    }
    
    public String getFamily(String api, IDocumentQuerier querier) {
        if(api==null||api.isEmpty()) {
            api = "default";
        }
        if(familyMap.containsKey(api)) {
            return familyMap.get(api);
        }
        String key = querier.getKey("service_families",api);
        Map map = querier.get(key, Map.class);
        if(map!=null) {
            String family = map.get("family").toString();
            familyMap.put(api, family);
            return family;
        }
        if(familyMap.containsKey("default")) {
            return familyMap.get("default");
        }
        key = querier.getKey("service_families","default");
        String family = querier.get(key, Map.class).get("family").toString();
        familyMap.put(api, family);
        return family;
    }
}
