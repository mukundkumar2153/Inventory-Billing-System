import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;

public class App {
    public static void main(String[] args) {
        int port = 8080;
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.trim().isEmpty()) {
            try {
                port = Integer.parseInt(portEnv.trim());
            } catch (NumberFormatException ignored) {}
        }

        try {
            Tomcat tomcat = new Tomcat();
            tomcat.setPort(port);
            tomcat.getConnector(); // Initialize HTTP connector

            String docBase = new File(".").getAbsolutePath();
            Context ctx = tomcat.addContext("", docBase);

            // Add Main servlet
            Tomcat.addServlet(ctx, "MainServlet", new Main());
            ctx.addServletMappingDecoded("/Main", "MainServlet");
            ctx.addServletMappingDecoded("/main", "MainServlet");
            ctx.addServletMappingDecoded("/style.css", "MainServlet");
            ctx.addServletMappingDecoded("/", "MainServlet");

            String appUrl = "http://localhost:" + port + "/Main";

            System.out.println("===============================================================");
            System.out.println("  SmartStore POS Server is running!");
            System.out.println("  URL: " + appUrl);
            System.out.println("===============================================================");

            tomcat.start();

            // Open browser automatically
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try {
                    Desktop.getDesktop().browse(new URI(appUrl));
                } catch (Exception ignored) {}
            }

            tomcat.getServer().await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
