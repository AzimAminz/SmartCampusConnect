package smartcampus.backend.config;

import jakarta.annotation.PostConstruct;
import jakarta.xml.ws.Endpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import smartcampus.backend.service.CampusBookingSoapService;

/**
 * Publishes the JAX-WS SOAP Endpoint on a separate port (8085).
 *
 * Satisfies R8 (SOAP/WSDL):
 *   WSDL URL: http://localhost:8085/ws/booking?wsdl
 *
 * Runs in a background thread so it does not block Spring Boot startup.
 */
@Component
public class SoapEndpointPublisher {

    @Value("${soap.server.port:8085}")
    private int soapPort;

    @Autowired
    private CampusBookingSoapService campusBookingSoapService;

    @PostConstruct
    public void publishSoapEndpoint() {
        Thread soapThread = new Thread(() -> {
            try {
                com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(
                    new java.net.InetSocketAddress("0.0.0.0", soapPort), 0
                );
                com.sun.net.httpserver.HttpContext context = server.createContext("/ws/booking");
                context.getFilters().add(new com.sun.net.httpserver.Filter() {
                    @Override
                    public String description() {
                        return "CORS Filter";
                    }

                    @Override
                    public void doFilter(com.sun.net.httpserver.HttpExchange exchange, Chain chain) throws java.io.IOException {
                        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
                        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Accept");
                        
                        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                            exchange.getResponseHeaders().set("Access-Control-Max-Age", "86400");
                            exchange.sendResponseHeaders(204, -1);
                            exchange.close();
                            return;
                        }
                        
                        chain.doFilter(exchange);
                    }
                });

                Endpoint endpoint = Endpoint.create(campusBookingSoapService);
                endpoint.publish(context);
                server.start();

                System.out.println("✅ [SOAP-SERVER] WSDL published with CORS support at: http://localhost:" + soapPort + "/ws/booking?wsdl");
                
                synchronized (this) {
                    this.wait();
                }
            } catch (Exception e) {
                System.err.println("❌ [SOAP-SERVER] Failed to publish endpoint: " + e.getMessage());
                e.printStackTrace();
            }
        });
        soapThread.setDaemon(true);
        soapThread.setName("soap-endpoint-publisher");
        soapThread.start();

        System.out.println("🚀 [SOAP-SERVER] Starting SOAP endpoint on port " + soapPort + " ...");
    }
}
