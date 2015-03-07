package ca.omny.ecs;

import ca.omny.potent.PowerServlet;
import java.util.List;
import java.util.Map;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

public class Main {
    public static void main(String[] args) throws Exception {
        Weld weld = new Weld();
        WeldContainer container = weld.initialize();
        int edgeRouterPort = 8080;
        if(System.getenv("omny_edge_port")!=null) {
            edgeRouterPort = Integer.parseInt(System.getenv("omny_edge_port"));
        }
        Server edgeServer = new Server(edgeRouterPort);
        PowerServlet edgeServlet = container.instance().select(PowerServlet.class).get();
        
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");

        context.addServlet(new ServletHolder(edgeServlet),"/*");
        edgeServer.setHandler(context);
        edgeServer.start();
        edgeServer.join();
    }
}
