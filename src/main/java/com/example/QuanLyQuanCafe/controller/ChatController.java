package com.example.QuanLyQuanCafe.controller;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.QuanLyQuanCafe.model.MenuItem;
import com.example.QuanLyQuanCafe.model.Staff;
import com.example.QuanLyQuanCafe.service.MenuService;
import com.example.QuanLyQuanCafe.service.StaffService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final MenuService menuService;
    private final StaffService staffService;

    // Đọc từ environment/properties: GEMINI_API_KEY -> gemini.api.key, GEMINI_MODEL -> gemini.model
    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    public ChatController(ObjectMapper objectMapper,
                          MenuService menuService,
                          StaffService staffService) {
        this.objectMapper = objectMapper;
        this.menuService = menuService;
        this.staffService = staffService;
    }

    @PostMapping("/ai")
    public ResponseEntity<?> chatWithAi(@RequestBody Map<String, Object> payload) {
        Object messageObj = payload.get("message");
        String message = messageObj != null ? messageObj.toString().trim() : "";

        if (message.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nội dung câu hỏi không được để trống"));
        }

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Chưa cấu hình GEMINI_API_KEY cho dịch vụ AI"));
        }

        try {
            String context = buildContextFromDatabase(message);
            String reply = callGemini(message, context);
            return ResponseEntity.ok(Map.of("reply", reply));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Không gọi được dịch vụ AI: " + ex.getMessage()));
        }
    }

    private String buildContextFromDatabase(String message) {
        String lower = message.toLowerCase();
        StringBuilder ctx = new StringBuilder();

        // Nếu người dùng hỏi về menu hoặc món, thêm danh sách món từ database
        if (lower.contains("menu") || lower.contains("thực đơn") || lower.contains("món") || lower.contains("đồ uống") || lower.contains("drink") || lower.contains("coffee")) {
            List<MenuItem> items = menuService.getAllItems();
            if (!items.isEmpty()) {
                ctx.append("DANH SÁCH MÓN TRONG HỆ THỐNG (tối đa 20 món):\n");
                String menuLines = items.stream()
                        .limit(20)
                        .map(i -> {
                            String price = i.getPrice() != null ? i.getPrice().toPlainString() + " đ" : "chưa có giá";
                            String status = i.getStatus() != null ? i.getStatus().name() : "UNKNOWN";
                            return "- " + i.getName() + " | giá: " + price + " | trạng thái: " + status;
                        })
                        .collect(Collectors.joining("\n"));
                ctx.append(menuLines).append("\n\n");
            }
        }

        // Nếu người dùng hỏi về nhân viên, thêm thông tin nhân viên
        if (lower.contains("nhân viên") || lower.contains("staff")) {
            List<Staff> staffList = staffService.findAll();
            if (!staffList.isEmpty()) {
                ctx.append("THÔNG TIN NHÂN VIÊN TRONG HỆ THỐNG (tối đa 20 người):\n");
                String staffLines = staffList.stream()
                        .limit(20)
                        .map(s -> {
                            String role = s.getRole() != null ? s.getRole().name() : "UNKNOWN";
                            String status = s.getStatus() != null ? s.getStatus().name() : "UNKNOWN";
                            String phone = s.getPhone() != null ? s.getPhone() : "không có";
                            return "- " + s.getName() + " | vai trò: " + role + " | trạng thái: " + status + " | SĐT: " + phone;
                        })
                        .collect(Collectors.joining("\n"));
                ctx.append(staffLines).append("\n\n");
            }
        }

        return ctx.toString().trim();
    }

    private String callGemini(String message, String context) throws IOException, InterruptedException {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + geminiModel + ":generateContent?key=" + geminiApiKey;

        StringBuilder userPrompt = new StringBuilder();
        if (context != null && !context.isBlank()) {
            userPrompt.append("Dữ liệu từ hệ thống quán cà phê (database):\n");
            userPrompt.append(context).append("\n\n");
        }
        userPrompt.append("Câu hỏi của người dùng: ").append(message);

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", "Bạn là trợ lý AI của quán cà phê Brew & Co. Hãy sử dụng dữ liệu cung cấp (nếu có) để trả lời chính xác. Trả lời bằng tiếng Việt, ngắn gọn, dễ hiểu."),
                                Map.of("text", userPrompt.toString())
                        ))
                )
        );

        String json = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Gemini trả về mã " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new IOException("Phản hồi Gemini không hợp lệ");
        }

        JsonNode contentNode = candidates.get(0).path("content");
        JsonNode parts = contentNode.path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            throw new IOException("Gemini không trả về nội dung");
        }

        StringBuilder sb = new StringBuilder();
        for (JsonNode part : parts) {
            String text = part.path("text").asText("");
            if (!text.isBlank()) {
                sb.append(text).append(" ");
            }
        }
        String result = sb.toString().trim();
        if (result.isEmpty()) {
            throw new IOException("Nội dung trả về trống");
        }
        return result;
    }
}
