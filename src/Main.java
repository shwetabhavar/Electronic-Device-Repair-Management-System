import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Main {

    // ==========================================
    // REPAIR CLASS
    // ==========================================

    static class Repair {

        String id;
        String customer;
        String device;
        String brand;
        String model;
        String problem;
        String status;
        String technician;

        Repair(String id,
               String customer,
               String device,
               String brand,
               String model,
               String problem) {

            this.id = id;
            this.customer = customer;
            this.device = device;
            this.brand = brand;
            this.model = model;
            this.problem = problem;

            this.status = "Pending";
            this.technician = "Not Assigned";
        }
    }


    // ==========================================
    // REPAIR STORAGE
    // ==========================================

    static List<Repair> repairs = new ArrayList<>();

    static final String DATA_FILE = "repairs.txt";


    // ==========================================
    // SAVE DATA TO FILE
    // ==========================================

    static void saveRepairs() {

        try {

            List<String> lines = new ArrayList<>();

            for (Repair r : repairs) {

                String line =
                        r.id + "~" +
                        r.customer + "~" +
                        r.device + "~" +
                        r.brand + "~" +
                        r.model + "~" +
                        r.problem + "~" +
                        r.status + "~" +
                        r.technician;

                lines.add(line);
            }

            Files.write(
                    new File(DATA_FILE).toPath(),
                    lines,
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {

            System.out.println(
                    "Error saving data: " +
                    e.getMessage()
            );
        }
    }


    // ==========================================
    // LOAD DATA FROM FILE
    // ==========================================

    static void loadRepairs() {

        try {

            File file = new File(DATA_FILE);

            if (!file.exists()) {
                return;
            }

            List<String> lines =
                    Files.readAllLines(
                            file.toPath(),
                            StandardCharsets.UTF_8
                    );

            repairs.clear();

            for (String line : lines) {

                String[] parts =
                        line.split("~", -1);

                if (parts.length == 8) {

                    Repair r =
                            new Repair(
                                    parts[0],
                                    parts[1],
                                    parts[2],
                                    parts[3],
                                    parts[4],
                                    parts[5]
                            );

                    r.status = parts[6];
                    r.technician = parts[7];

                    repairs.add(r);
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "Error loading data: " +
                    e.getMessage()
            );
        }
    }


    // ==========================================
    // MAIN METHOD
    // ==========================================

    public static void main(String[] args) throws Exception {

        // Load old saved data
        loadRepairs();


        // ==========================================
        // FIRST TIME SAMPLE DATA
        // ==========================================

        if (repairs.isEmpty()) {

            repairs.add(
                    new Repair(
                            "R001",
                            "Rahul",
                            "Laptop",
                            "Dell",
                            "Inspiron",
                            "Not Starting"
                    )
            );

            repairs.add(
                    new Repair(
                            "R002",
                            "Amit",
                            "Mobile",
                            "Samsung",
                            "Galaxy",
                            "Screen Problem"
                    )
            );

            saveRepairs();
        }


        // ==========================================
        // CREATE SERVER
        // ==========================================

        HttpServer server =
                HttpServer.create(
                        new InetSocketAddress(8082),
                        0
                );


        // ==========================================
        // WEBSITE
        // ==========================================

        server.createContext("/", exchange -> {

            String path =
                    exchange.getRequestURI().getPath();

            if (path.equals("/")) {
                path = "/index.html";
            }

            File file =
                    new File("web" + path);

            if (file.exists() &&
                    !file.isDirectory()) {

                byte[] data =
                        Files.readAllBytes(
                                file.toPath()
                        );

                String contentType =
                        "text/html";

                if (path.endsWith(".css")) {

                    contentType = "text/css";

                } else if (path.endsWith(".js")) {

                    contentType =
                            "application/javascript";
                }

                exchange.getResponseHeaders()
                        .set(
                                "Content-Type",
                                contentType
                        );

                exchange.sendResponseHeaders(
                        200,
                        data.length
                );

                OutputStream output =
                        exchange.getResponseBody();

                output.write(data);
                output.close();

            } else {

                String message =
                        "404 - Page Not Found";

                byte[] data =
                        message.getBytes(
                                StandardCharsets.UTF_8
                        );

                exchange.sendResponseHeaders(
                        404,
                        data.length
                );

                OutputStream output =
                        exchange.getResponseBody();

                output.write(data);
                output.close();
            }

        });


        // ==========================================
        // GET ALL REPAIRS
        // ==========================================

        server.createContext(
                "/api/repairs",
                exchange -> {

            StringBuilder json =
                    new StringBuilder();

            json.append("[");

            for (int i = 0;
                 i < repairs.size();
                 i++) {

                Repair r =
                        repairs.get(i);

                json.append("{");

                json.append("\"id\":\"")
                        .append(r.id)
                        .append("\",");

                json.append("\"customer\":\"")
                        .append(r.customer)
                        .append("\",");

                json.append("\"device\":\"")
                        .append(r.device)
                        .append("\",");

                json.append("\"brand\":\"")
                        .append(r.brand)
                        .append("\",");

                json.append("\"model\":\"")
                        .append(r.model)
                        .append("\",");

                json.append("\"problem\":\"")
                        .append(r.problem)
                        .append("\",");

                json.append("\"status\":\"")
                        .append(r.status)
                        .append("\",");

                json.append("\"technician\":\"")
                        .append(r.technician)
                        .append("\"");

                json.append("}");

                if (i < repairs.size() - 1) {
                    json.append(",");
                }
            }

            json.append("]");

            byte[] data =
                    json.toString()
                            .getBytes(
                                    StandardCharsets.UTF_8
                            );

            exchange.getResponseHeaders()
                    .set(
                            "Content-Type",
                            "application/json"
                    );

            exchange.sendResponseHeaders(
                    200,
                    data.length
            );

            OutputStream output =
                    exchange.getResponseBody();

            output.write(data);
            output.close();

        });


        // ==========================================
        // CREATE NEW REPAIR REQUEST
        // ==========================================

        server.createContext(
                "/api/repairs/create",
                exchange -> {

            if (!exchange
                    .getRequestMethod()
                    .equalsIgnoreCase("POST")) {

                String message =
                        "Only POST method is allowed";

                byte[] data =
                        message.getBytes(
                                StandardCharsets.UTF_8
                        );

                exchange.sendResponseHeaders(
                        405,
                        data.length
                );

                OutputStream output =
                        exchange.getResponseBody();

                output.write(data);
                output.close();

                return;
            }


            // Read form data

            String formData =
                    new String(
                            exchange.getRequestBody()
                                    .readAllBytes(),
                            StandardCharsets.UTF_8
                    );


            // ==========================================
            // FORM FIELDS
            // ==========================================

            String customer = "";
            String device = "";
            String brand = "";
            String model = "";
            String problem = "";


            String[] fields =
                    formData.split("&");


            for (String field : fields) {

                String[] parts =
                        field.split("=", 2);

                if (parts.length < 2) {
                    continue;
                }

                String key =
                        parts[0];

                String value =
                        java.net.URLDecoder.decode(
                                parts[1],
                                StandardCharsets.UTF_8
                        );


                if (key.equals("customer")) {

                    customer = value;

                } else if (key.equals("device")) {

                    device = value;

                } else if (key.equals("brand")) {

                    brand = value;

                } else if (key.equals("model")) {

                    model = value;

                } else if (key.equals("problem")) {

                    problem = value;
                }
            }


            // ==========================================
            // CUSTOMER VALIDATION
            // ==========================================

            if (customer.trim().isEmpty()) {

                customer = "customer";
            }


            // ==========================================
            // GENERATE REQUEST ID
            // ==========================================

            String requestId =
                    "R" +
                    String.format(
                            "%03d",
                            repairs.size() + 1
                    );


            // ==========================================
            // CREATE REPAIR
            // ==========================================

            Repair newRepair =
                    new Repair(
                            requestId,
                            customer,
                            device,
                            brand,
                            model,
                            problem
                    );


            repairs.add(newRepair);


            // ==========================================
            // SAVE PERMANENTLY
            // ==========================================

            saveRepairs();


            // ==========================================
            // RESPONSE
            // ==========================================

            String response =
                    "Repair request created successfully. " +
                    "Request ID: " +
                    requestId;


            byte[] data =
                    response.getBytes(
                            StandardCharsets.UTF_8
                    );


            exchange.getResponseHeaders()
                    .set(
                            "Content-Type",
                            "text/plain"
                    );


            exchange.sendResponseHeaders(
                    200,
                    data.length
            );


            OutputStream output =
                    exchange.getResponseBody();

            output.write(data);
            output.close();

        });


        // ==========================================
        // ASSIGN TECHNICIAN
        // ==========================================

        server.createContext(
                "/api/repairs/assign",
                exchange -> {

            if (!exchange
                    .getRequestMethod()
                    .equalsIgnoreCase("POST")) {

                String message =
                        "Only POST method is allowed";

                byte[] data =
                        message.getBytes(
                                StandardCharsets.UTF_8
                        );

                exchange.sendResponseHeaders(
                        405,
                        data.length
                );

                OutputStream output =
                        exchange.getResponseBody();

                output.write(data);
                output.close();

                return;
            }


            String formData =
                    new String(
                            exchange.getRequestBody()
                                    .readAllBytes(),
                            StandardCharsets.UTF_8
                    );


            String repairId = "";
            String technician = "";


            String[] fields =
                    formData.split("&");


            for (String field : fields) {

                String[] parts =
                        field.split("=", 2);

                if (parts.length < 2) {
                    continue;
                }

                String key =
                        parts[0];

                String value =
                        java.net.URLDecoder.decode(
                                parts[1],
                                StandardCharsets.UTF_8
                        );


                if (key.equals("repair")) {

                    repairId = value;

                } else if (key.equals("technician")) {

                    technician = value;
                }
            }


            Repair selectedRepair = null;


            for (Repair r : repairs) {

                if (r.id.equals(repairId)) {

                    selectedRepair = r;
                    break;
                }
            }


            String response;


            if (selectedRepair != null) {

                selectedRepair.technician =
                        technician;

                selectedRepair.status =
                        "Assigned";


                // Save permanently

                saveRepairs();


                response =
                        "Technician " +
                        technician +
                        " assigned to Repair Request " +
                        repairId;

            } else {

                response =
                        "Repair Request not found";
            }


            byte[] data =
                    response.getBytes(
                            StandardCharsets.UTF_8
                    );


            exchange.getResponseHeaders()
                    .set(
                            "Content-Type",
                            "text/plain"
                    );


            exchange.sendResponseHeaders(
                    200,
                    data.length
            );


            OutputStream output =
                    exchange.getResponseBody();

            output.write(data);
            output.close();

        });


        // ==========================================
        // GET ASSIGNED REPAIRS
        // ==========================================

        server.createContext(
                "/api/repairs/assigned",
                exchange -> {

            String technicianId =
                    "T001";


            StringBuilder json =
                    new StringBuilder();


            json.append("[");


            boolean first = true;


            for (Repair r : repairs) {

                if (r.technician
                        .equals(technicianId)) {

                    if (!first) {
                        json.append(",");
                    }


                    json.append("{");


                    json.append("\"id\":\"")
                            .append(r.id)
                            .append("\",");


                    json.append("\"customer\":\"")
                            .append(r.customer)
                            .append("\",");


                    json.append("\"device\":\"")
                            .append(r.device)
                            .append("\",");


                    json.append("\"problem\":\"")
                            .append(r.problem)
                            .append("\",");


                    json.append("\"status\":\"")
                            .append(r.status)
                            .append("\",");


                    json.append("\"technician\":\"")
                            .append(r.technician)
                            .append("\"");


                    json.append("}");


                    first = false;
                }
            }


            json.append("]");


            byte[] data =
                    json.toString()
                            .getBytes(
                                    StandardCharsets.UTF_8
                            );


            exchange.getResponseHeaders()
                    .set(
                            "Content-Type",
                            "application/json"
                    );


            exchange.sendResponseHeaders(
                    200,
                    data.length
            );


            OutputStream output =
                    exchange.getResponseBody();

            output.write(data);
            output.close();

        });


        // ==========================================
        // UPDATE REPAIR STATUS
        // ==========================================

        server.createContext(
                "/api/repairs/status",
                exchange -> {

            if (!exchange
                    .getRequestMethod()
                    .equalsIgnoreCase("POST")) {

                String message =
                        "Only POST method is allowed";

                byte[] data =
                        message.getBytes(
                                StandardCharsets.UTF_8
                        );

                exchange.sendResponseHeaders(
                        405,
                        data.length
                );

                OutputStream output =
                        exchange.getResponseBody();

                output.write(data);
                output.close();

                return;
            }


            String formData =
                    new String(
                            exchange.getRequestBody()
                                    .readAllBytes(),
                            StandardCharsets.UTF_8
                    );


            String repairId = "";
            String status = "";


            String[] fields =
                    formData.split("&");


            for (String field : fields) {

                String[] parts =
                        field.split("=", 2);

                if (parts.length < 2) {
                    continue;
                }

                String key =
                        parts[0];

                String value =
                        java.net.URLDecoder.decode(
                                parts[1],
                                StandardCharsets.UTF_8
                        );


                if (key.equals("repair")) {

                    repairId = value;

                } else if (key.equals("status")) {

                    status = value;
                }
            }


            Repair selectedRepair = null;


            for (Repair r : repairs) {

                if (r.id.equals(repairId)) {

                    selectedRepair = r;
                    break;
                }
            }


            String response;


            if (selectedRepair != null) {

                selectedRepair.status =
                        status;


                // Save permanently

                saveRepairs();


                response =
                        "Repair " +
                        repairId +
                        " status updated to: " +
                        status;

            } else {

                response =
                        "Repair Request not found";
            }


            byte[] data =
                    response.getBytes(
                            StandardCharsets.UTF_8
                    );


            exchange.getResponseHeaders()
                    .set(
                            "Content-Type",
                            "text/plain"
                    );


            exchange.sendResponseHeaders(
                    200,
                    data.length
            );


            OutputStream output =
                    exchange.getResponseBody();

            output.write(data);
            output.close();

        });


        // ==========================================
        // START SERVER
        // ==========================================

        server.start();


        System.out.println(
                "Server started: http://localhost:8082"
        );
    }
}