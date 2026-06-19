package smartcampus.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Booking & Library Microservice — R8 (SOAP/WSDL)
 *
 * Exposes:
 *   - Port 8082 : REST API (room bookings, book catalog)
 *   - Port 8085 : SOAP/WSDL Endpoint (JAX-WS)
 */
@SpringBootApplication
public class BookingServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApp.class, args);
    }
}
