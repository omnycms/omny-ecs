package ca.omny.ecs.lambda;

import ca.omny.configuration.ConfigurationReader;
import ca.omny.documentdb.IDocumentQuerier;
import ca.omny.documentdb.QuerierFactory;
import ca.omny.extension.proxy.IOmnyProxyService;
import ca.omny.lambda.wrapper.models.FakeHttpResponse;
import ca.omny.lambda.wrapper.models.LambdaInput;
import ca.omny.lambda.wrapper.models.LambdaOutput;
import ca.omny.potent.HeaderManager;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LambdaProxy implements IOmnyProxyService {

    String routingPattern;
    ConfigurationReader configurationReader = ConfigurationReader.getDefaultConfigurationReader();
    AWSLambdaClient lambda;
    IDocumentQuerier querier = QuerierFactory.getDefaultQuerier();

    public LambdaProxy() {
        lambda = new AWSLambdaClient();
    }

    @Override
    public void proxyRequest(String hostHeader, String uid, Map configuration, HttpServletRequest req, HttpServletResponse resp) throws MalformedURLException, IOException {
        Gson gson = new Gson();
        LambdaFunctionDetails functionDetails = gson.fromJson(gson.toJson(configuration.get("configuration")), LambdaFunctionDetails.class);
        LambdaInput parameters = new LambdaInput();
        parameters.setConfigOverrides(functionDetails.getConfigOverrides());
        HeaderManager headerManager = new HeaderManager();
        parameters.setHeaders(headerManager.getSendableHeaders(hostHeader, uid, req));
        parameters.setMethod(req.getMethod());
        parameters.setUri(req.getRequestURI());
        InvokeRequest invokeRequest = new InvokeRequest()
                .withFunctionName(functionDetails.getArn())
                .withPayload(gson.toJson(parameters));
        InvokeResult result = lambda.invoke(invokeRequest);
        String payload = new String(result.getPayload().array(), Charset.forName("UTF-8"));
        
        FakeHttpResponse output = gson.fromJson(gson.fromJson(payload,String.class), FakeHttpResponse.class);
        resp.getWriter().write(output.getBody());
        resp.setStatus(output.getStatus());
        if (output.getHeaders() != null) {
            for (String header : output.getHeaders().keySet()) {
                resp.setHeader(header, output.getHeaders().get(header));
            }
        }
        if (output.getCookies() != null) {
            for (String cookie : output.getCookies().keySet()) {
                resp.addCookie(output.getCookies().get(cookie));
            }
        }
    }

    @Override
    public String getRoutingPattern() {
        return null;
    }

    @Override
    public String getId() {
        return "LAMBDA";
    }

}
