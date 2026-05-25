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
        String url = "http://0.0.0.0:" + soapPort + "/ws/booking";

        Thread soapThread = new Thread(() -> {
            try {
                Endpoint endpoint = Endpoint.publish(url, campusBookingSoapService);
                System.out.println("✅ [SOAP-SERVER] WSDL published at: " + url + "?wsdl");
                // Keep thread alive — endpoint is active as long as thread runs
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
