package ca.omny.ecs.lambda;

import java.util.Map;

public class LambdaFunctionDetails {
    String arn;
    Map<String,String> configOverrides;

    public String getArn() {
        return arn;
    }

    public void setArn(String arn) {
        this.arn = arn;
    }

    public Map<String, String> getConfigOverrides() {
        return configOverrides;
    }

    public void setConfigOverrides(Map<String, String> configOverrides) {
        this.configOverrides = configOverrides;
    }
}
