package service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import model.Book;
import model.BookLoan;

public class SoapService {
    private static final String SOAP_URL = "http://localhost:8085/ws/booking";
    private final HttpClient client = HttpClient.newHttpClient();

    private String sendSoapRequest(String xmlPayload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SOAP_URL))
                .header("Content-Type", "text/xml; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(xmlPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        String body = response.body();
        if (response.statusCode() != 200) {
            String fault = checkAndGetSoapFault(body);
            if (fault != null) {
                throw new Exception(fault);
            }
            throw new Exception("SOAP Request failed with status " + response.statusCode());
        }
        
        String fault = checkAndGetSoapFault(body);
        if (fault != null) {
            throw new Exception(fault);
        }
        
        return body;
    }

    private String checkAndGetSoapFault(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            NodeList nl = doc.getElementsByTagName("faultstring");
            if (nl != null && nl.getLength() > 0) {
                String fault = nl.item(0).getTextContent();
                if (fault != null) {
                    if (fault.startsWith("SOAP_FAULT: ")) {
                        fault = fault.substring("SOAP_FAULT: ".length());
                    } else if (fault.startsWith("SOAP_FAULT:")) {
                        fault = fault.substring("SOAP_FAULT:".length());
                    }
                }
                return fault;
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private static String getTagValue(Element el, String tagName) {
        NodeList nl = el.getElementsByTagName(tagName);
        if (nl != null && nl.getLength() > 0 && nl.item(0) != null) {
            return nl.item(0).getTextContent();
        }
        return "";
    }

    public List<Book> searchBooks(String query) throws Exception {
        String xmlPayload = 
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://service.smartcampus/\">" +
            "   <soapenv:Header/>" +
            "   <soapenv:Body>" +
            "      <ser:searchBooks>" +
            "         <query>" + query + "</query>" +
            "      </ser:searchBooks>" +
            "   </soapenv:Body>" +
            "</soapenv:Envelope>";

        String xmlResponse = sendSoapRequest(xmlPayload);
        
        List<Book> books = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8)));
        NodeList list = doc.getElementsByTagName("return");
        
        for (int i = 0; i < list.getLength(); i++) {
            Element el = (Element) list.item(i);
            int id = Integer.parseInt(getTagValue(el, "id"));
            String isbn = getTagValue(el, "isbn");
            String title = getTagValue(el, "title");
            String author = getTagValue(el, "author");
            String category = getTagValue(el, "category");
            String status = getTagValue(el, "status");
            books.add(new Book(id, isbn, title, author, category, status));
        }
        
        return books;
    }

    public boolean addBook(String token, String isbn, String title, String author, String category) throws Exception {
        String xmlPayload = 
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://service.smartcampus/\">" +
            "   <soapenv:Header/>" +
            "   <soapenv:Body>" +
            "      <ser:addBook>" +
            "         <token>" + token + "</token>" +
            "         <isbn>" + isbn + "</isbn>" +
            "         <title>" + title + "</title>" +
            "         <author>" + author + "</author>" +
            "         <category>" + category + "</category>" +
            "      </ser:addBook>" +
            "   </soapenv:Body>" +
            "</soapenv:Envelope>";

        String xmlResponse = sendSoapRequest(xmlPayload);
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8)));
        NodeList nl = doc.getElementsByTagName("return");
        if (nl != null && nl.getLength() > 0) {
            return "true".equalsIgnoreCase(nl.item(0).getTextContent());
        }
        return false;
    }

    public String borrowBook(String token, String studentId, String studentName, String isbn, String dueDate) throws Exception {
        String xmlPayload = 
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://service.smartcampus/\">" +
            "   <soapenv:Header/>" +
            "   <soapenv:Body>" +
            "      <ser:borrowBook>" +
            "         <token>" + token + "</token>" +
            "         <studentId>" + studentId + "</studentId>" +
            "         <studentName>" + studentName + "</studentName>" +
            "         <isbn>" + isbn + "</isbn>" +
            "         <dueDate>" + dueDate + "</dueDate>" +
            "      </ser:borrowBook>" +
            "   </soapenv:Body>" +
            "</soapenv:Envelope>";

        String xmlResponse = sendSoapRequest(xmlPayload);
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8)));
        NodeList nl = doc.getElementsByTagName("return");
        if (nl != null && nl.getLength() > 0) {
            return nl.item(0).getTextContent();
        }
        throw new Exception("No loan reference returned");
    }

    public boolean returnBook(String token, String loanRef) throws Exception {
        String xmlPayload = 
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://service.smartcampus/\">" +
            "   <soapenv:Header/>" +
            "   <soapenv:Body>" +
            "      <ser:returnBook>" +
            "         <token>" + token + "</token>" +
            "         <loanRef>" + loanRef + "</loanRef>" +
            "      </ser:returnBook>" +
            "   </soapenv:Body>" +
            "</soapenv:Envelope>";

        String xmlResponse = sendSoapRequest(xmlPayload);
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8)));
        NodeList nl = doc.getElementsByTagName("return");
        if (nl != null && nl.getLength() > 0) {
            return "true".equalsIgnoreCase(nl.item(0).getTextContent());
        }
        return false;
    }

    public List<BookLoan> getBookLoanHistory(String token, String isbn) throws Exception {
        String xmlPayload = 
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://service.smartcampus/\">" +
            "   <soapenv:Header/>" +
            "   <soapenv:Body>" +
            "      <ser:getBookLoanHistory>" +
            "         <token>" + token + "</token>" +
            "         <isbn>" + isbn + "</isbn>" +
            "      </ser:getBookLoanHistory>" +
            "   </soapenv:Body>" +
            "</soapenv:Envelope>";

        String xmlResponse = sendSoapRequest(xmlPayload);
        return parseBookLoans(xmlResponse);
    }

    public List<BookLoan> getStudentLoanHistory(String token, String studentId) throws Exception {
        String xmlPayload = 
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://service.smartcampus/\">" +
            "   <soapenv:Header/>" +
            "   <soapenv:Body>" +
            "      <ser:getStudentLoanHistory>" +
            "         <token>" + token + "</token>" +
            "         <studentId>" + studentId + "</studentId>" +
            "      </ser:getStudentLoanHistory>" +
            "   </soapenv:Body>" +
            "</soapenv:Envelope>";

        String xmlResponse = sendSoapRequest(xmlPayload);
        return parseBookLoans(xmlResponse);
    }

    public String bookRoom(String studentId, String studentName, String roomName, String slot, String date, String purpose) throws Exception {
        String xmlPayload = 
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://service.smartcampus/\">" +
            "   <soapenv:Header/>" +
            "   <soapenv:Body>" +
            "      <ser:bookRoom>" +
            "         <studentId>" + studentId + "</studentId>" +
            "         <studentName>" + studentName + "</studentName>" +
            "         <roomName>" + roomName + "</roomName>" +
            "         <slot>" + slot + "</slot>" +
            "         <date>" + date + "</date>" +
            "         <purpose>" + purpose + "</purpose>" +
            "      </ser:bookRoom>" +
            "   </soapenv:Body>" +
            "</soapenv:Envelope>";

        String xmlResponse = sendSoapRequest(xmlPayload);
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8)));
        NodeList nl = doc.getElementsByTagName("return");
        if (nl != null && nl.getLength() > 0) {
            return nl.item(0).getTextContent();
        }
        throw new Exception("No booking reference returned");
    }

    public boolean checkAvailability(String roomName, String slot, String date) throws Exception {
        String xmlPayload = 
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://service.smartcampus/\">" +
            "   <soapenv:Header/>" +
            "   <soapenv:Body>" +
            "      <ser:checkAvailability>" +
            "         <roomName>" + roomName + "</roomName>" +
            "         <slot>" + slot + "</slot>" +
            "         <date>" + date + "</date>" +
            "      </ser:checkAvailability>" +
            "   </soapenv:Body>" +
            "</soapenv:Envelope>";

        String xmlResponse = sendSoapRequest(xmlPayload);
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8)));
        NodeList nl = doc.getElementsByTagName("return");
        if (nl != null && nl.getLength() > 0) {
            return "true".equalsIgnoreCase(nl.item(0).getTextContent());
        }
        return false;
    }

    public boolean cancelBooking(String bookingRef) throws Exception {
        String xmlPayload = 
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://service.smartcampus/\">" +
            "   <soapenv:Header/>" +
            "   <soapenv:Body>" +
            "      <ser:cancelBooking>" +
            "         <bookingRef>" + bookingRef + "</bookingRef>" +
            "      </ser:cancelBooking>" +
            "   </soapenv:Body>" +
            "</soapenv:Envelope>";

        String xmlResponse = sendSoapRequest(xmlPayload);
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8)));
        NodeList nl = doc.getElementsByTagName("return");
        if (nl != null && nl.getLength() > 0) {
            return "true".equalsIgnoreCase(nl.item(0).getTextContent());
        }
        return false;
    }

    private List<BookLoan> parseBookLoans(String xmlResponse) throws Exception {
        List<BookLoan> loans = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8)));
        NodeList list = doc.getElementsByTagName("return");
        
        for (int i = 0; i < list.getLength(); i++) {
            Element el = (Element) list.item(i);
            int id = Integer.parseInt(getTagValue(el, "id"));
            String loanReference = getTagValue(el, "loanReference");
            String studentId = getTagValue(el, "studentId");
            String studentName = getTagValue(el, "studentName");
            String bookIsbn = getTagValue(el, "bookIsbn");
            String bookTitle = getTagValue(el, "bookTitle");
            String loanDate = getTagValue(el, "loanDate");
            String dueDate = getTagValue(el, "dueDate");
            String returnDate = getTagValue(el, "returnDate");
            String status = getTagValue(el, "status");
            
            double fineAmount = 0.0;
            String fineStr = getTagValue(el, "fineAmount");
            if (!fineStr.isEmpty()) {
                fineAmount = Double.parseDouble(fineStr);
            }
            
            loans.add(new BookLoan(id, loanReference, studentId, studentName, bookIsbn, bookTitle, loanDate, dueDate, returnDate, status, fineAmount));
        }
        
        return loans;
    }
}
