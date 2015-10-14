package ca.omny.ecs;

import ca.omny.configuration.ConfigurationReader;
import ca.omny.potent.PowerServlet;
import ca.omny.server.OmnyClassRegister;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class Main {
    public static void main(String[] args) throws Exception {

        int edgeRouterPort = 8080;
        ConfigurationReader configurationReader = ConfigurationReader.getDefaultConfigurationReader();
        configurationReader.setKey("OMNY_NO_INJECTION", "true");
        configurationReader.setKey("OMNY_LOAD_CLASSES", "[\"ca.omny.db.extended.ExtendedDatabaseFactory\",\"ca.omny.potent.RegisterApis\",\"ca.omny.ecs.RegisterProviders\"]");
        new OmnyClassRegister().loadFromEnvironment();
        if(System.getenv("OMNY_EDGE_PORT")!=null) {
            edgeRouterPort = Integer.parseInt(System.getenv("OMNY_EDGE_PORT"));
        }
        Server edgeServer = new Server(edgeRouterPort);
        PowerServlet edgeServlet = new PowerServlet();
        
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");

        context.addServlet(new ServletHolder(edgeServlet),"/*");
        edgeServer.setHandler(context);
        edgeServer.start();
        edgeServer.join();
    }
}
