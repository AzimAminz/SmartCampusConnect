package service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class RestService {
    private static final String BASE_URL = "http://localhost:8080/api";
    private final HttpClient client = HttpClient.newHttpClient();

    public String login(String userId) throws Exception {

        // Prepare the JSON data
        // userId: xxx
        String jsonPayload = "{\"userId\":\"" + userId + "\"}";

        // Prepare the URL to be run
        // Will auto connect to backend controller folder's AuthController.java
        // How it knows connect to AuthController.java even if below takde mention?
        // Look the URL, it has /auth/login, so the system will auto locate this URL mapping to which controller file
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        // Send the request to the backend controller "AuthController.java"
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new Exception("Authentication failed: Status " + response.statusCode() + " - " + response.body());
        }
    }

    public String getDashboardData(String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/dashboard"))
                .header("X-Auth-Token", token)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new Exception("Failed to fetch dashboard: " + response.body());
        }
    }

    public String getStudents(String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/students"))
                .header("X-Auth-Token", token)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new Exception("Failed to fetch students: " + response.body());
        }
    }

    public String getCourses(String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/courses"))
                .header("X-Auth-Token", token)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new Exception("Failed to fetch courses: " + response.body());
        }
    }

    public String enrolStudent(String token, String studentId, String courseCode) throws Exception {
        String jsonPayload = "{\"studentId\":" + studentId + ",\"courseCode\":\"" + courseCode + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/enrol"))
                .header("X-Auth-Token", token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 201 || response.statusCode() == 200) {
            return response.body();
        } else {
            throw new Exception("Enrolment failed: " + response.body());
        }
    }

    public String dropCourse(String token, String studentId, String courseCode) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/enrol/" + studentId + "/" + courseCode))
                .header("X-Auth-Token", token)
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new Exception("Failed to drop course: " + response.body());
        }
    }

    public String getReportingStats(String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/reporting/stats"))
                .header("X-Auth-Token", token)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new Exception("Failed to fetch reporting stats: " + response.body());
        }
    }

    public String getEnrolmentsPerCourse(String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/reporting/enrolments-per-course"))
                .header("X-Auth-Token", token)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new Exception("Failed to fetch enrolments per course: " + response.body());
        }
    }

    public String addStudent(String token, String name, String email, String programme, String faculty, String semester, String gpa, String phoneNumber, String programmeCode) throws Exception {
        String jsonPayload = String.format(
            "{\"name\":\"%s\",\"email\":\"%s\",\"programme\":\"%s\",\"faculty\":\"%s\",\"semester\":\"%s\",\"gpa\":%s,\"phoneNumber\":\"%s\",\"programmeCode\":\"%s\"}",
            name, email, programme, faculty, semester, gpa, phoneNumber, programmeCode
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/students"))
                .header("X-Auth-Token", token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 201 || response.statusCode() == 200) {
            return response.body();
        } else {
            throw new Exception("Failed to add student: " + response.body());
        }
    }

    public String updateStudent(String token, String id, String name, String email, String programme, String faculty, String semester, String gpa, String phoneNumber) throws Exception {
        String jsonPayload = String.format(
            "{\"name\":\"%s\",\"email\":\"%s\",\"programme\":\"%s\",\"faculty\":\"%s\",\"semester\":\"%s\",\"gpa\":%s,\"phoneNumber\":\"%s\"}",
            name, email, programme, faculty, semester, gpa, phoneNumber
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/students/" + id))
                .header("X-Auth-Token", token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new Exception("Failed to update student: " + response.body());
        }
    }

    public String deleteStudent(String token, String id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/students/" + id))
                .header("X-Auth-Token", token)
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 204 || response.statusCode() == 200) {
            return "Student deleted successfully";
        } else {
            throw new Exception("Failed to delete student: " + response.body());
        }
    }

    public String getStudentProfile(String token, String studentId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/reporting/student/" + studentId))
                .header("X-Auth-Token", token)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new Exception("Student not found: " + response.body());
        }
    }

    public String addCourse(String token, String courseCode, String courseTitle, String lecturer, String faculty, String creditHours, String maxCapacity, String semester) throws Exception {
        String jsonPayload = String.format(
            "{\"courseCode\":\"%s\",\"courseTitle\":\"%s\",\"lecturer\":\"%s\",\"faculty\":\"%s\",\"creditHours\":%s,\"maxCapacity\":%s,\"semester\":\"%s\"}",
            courseCode, courseTitle, lecturer, faculty, creditHours, maxCapacity, semester
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/courses"))
                .header("X-Auth-Token", token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 201 || response.statusCode() == 200) {
            return response.body();
        } else {
            throw new Exception("Failed to add course: " + response.body());
        }
    }

    public String updateCourse(String token, String id, String courseTitle, String lecturer, String faculty, String creditHours, String maxCapacity, String semester) throws Exception {
        String jsonPayload = String.format(
            "{\"courseTitle\":\"%s\",\"lecturer\":\"%s\",\"faculty\":\"%s\",\"creditHours\":%s,\"maxCapacity\":%s,\"semester\":\"%s\"}",
            courseTitle, lecturer, faculty, creditHours, maxCapacity, semester
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/courses/" + id))
                .header("X-Auth-Token", token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new Exception("Failed to update course: " + response.body());
        }
    }

    public String deleteCourse(String token, String id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/courses/" + id))
                .header("X-Auth-Token", token)
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new Exception("Failed to delete course: " + response.body());
        }
    }
}

