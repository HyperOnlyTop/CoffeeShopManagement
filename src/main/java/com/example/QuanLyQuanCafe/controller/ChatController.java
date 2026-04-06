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
import com.example.QuanLyQuanCafe.service.MenuService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final MenuService menuService;

    // Đọc từ environment/properties: GEMINI_API_KEY -> gemini.api.key, GEMINI_MODEL -> gemini.model
    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    public ChatController(ObjectMapper objectMapper,
                          MenuService menuService) {
        this.objectMapper = objectMapper;
        this.menuService = menuService;
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

        List<MenuItem> items = menuService.getAllItems();
        if (items.isEmpty()) {
            return "";
        }

        // Kiểm tra xem khách có hỏi về món cụ thể không
        boolean askingSpecificItem = lower.contains("giá") || lower.contains("bao nhiêu") 
                || lower.contains("price") || lower.contains("gợi ý") || lower.contains("recommend");
        
        // Tìm các món khớp với từ khóa trong câu hỏi
        List<MenuItem> matchedItems = items.stream()
                .filter(i -> i.getName() != null && lower.contains(i.getName().toLowerCase()))
                .collect(Collectors.toList());

        if (!matchedItems.isEmpty()) {
            // Khách hỏi về món cụ thể - chỉ trả về món đó
            ctx.append("Món khách hỏi:\n");
            for (MenuItem i : matchedItems) {
                String price = i.getPrice() != null ? i.getPrice().toPlainString() + " đ" : "chưa có giá";
                ctx.append("- ").append(i.getName()).append(": ").append(price).append("\n");
            }
        } else if (askingSpecificItem) {
            // Khách muốn gợi ý hoặc hỏi giá nhưng không nêu tên món cụ thể
            // Chỉ gửi danh mục và một vài món tiêu biểu
            ctx.append("Các danh mục chính: Cà phê, Trà, Matcha, Sinh tố, Nước ép\n");
            ctx.append("Một số món phổ biến:\n");
            String popular = items.stream()
                    .limit(8)
                    .map(i -> "- " + i.getName() + ": " + (i.getPrice() != null ? i.getPrice().toPlainString() + " đ" : ""))
                    .collect(Collectors.joining("\n"));
            ctx.append(popular);
        }
        // Nếu khách chỉ hỏi chung về menu -> không gửi data, để AI tự trả lời theo prompt

        return ctx.toString().trim();
    }

    private static final String SYSTEM_PROMPT = """
        Bạn là trợ lý AI của quán cà phê Brew & Co., hỗ trợ KHÁCH HÀNG.
        
        BẠN CHỈ ĐƯỢC TRẢ LỜI VỀ:
        - Menu, thực đơn, đồ uống, món ăn, giá cả
        - Thông tin quán: địa chỉ, giờ mở cửa, cách đặt bàn
        - Gợi ý đồ uống phù hợp với sở thích khách
        - Khuyến mãi, ưu đãi (nếu có trong dữ liệu)
        
        BẠN TUYỆT ĐỐI KHÔNG ĐƯỢC TRẢ LỜI VỀ:
        - Thông tin nhân viên, lương, ca làm việc
        - Doanh thu, lợi nhuận, báo cáo tài chính
        - Thông tin quản trị, admin, hệ thống
        - Thông tin khách hàng khác
        - Mật khẩu, tài khoản, bảo mật
        - Kho hàng, nguyên liệu, nhà cung cấp
        
        CÁCH TRẢ LỜI VỀ MENU (RẤT QUAN TRỌNG):
        - KHÔNG BAO GIỜ liệt kê toàn bộ menu - quá dài và khó đọc trong chat
        - Nếu khách hỏi chung "menu có gì", "thực đơn": chỉ giới thiệu CÁC DANH MỤC CHÍNH (Cà phê, Trà, Matcha, Sinh tố,...) và mời xem chi tiết tại mục Thực đơn trên trang chủ
        - Nếu khách hỏi MỘT MÓN CỤ THỂ (VD: "Latte bao nhiêu"): trả lời giá món đó
        - Nếu khách muốn GỢI Ý: hỏi sở thích rồi gợi ý 2-3 món, không liệt kê nhiều
        
        Nếu khách hỏi về các chủ đề bị cấm, hãy lịch sự từ chối và gợi ý liên hệ qua tab "Chat với Nhân viên".
        
        Trả lời bằng tiếng Việt, NGẮN GỌN (2-4 câu), thân thiện.
        Thông tin quán: Brew & Co. - Thủ Đức, TP.HCM - SĐT: 0914070309 - Giờ mở cửa: 7:00 - 22:00.
        """;

    private String callGemini(String message, String context) throws IOException, InterruptedException {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + geminiModel + ":generateContent?key=" + geminiApiKey;

        StringBuilder userPrompt = new StringBuilder();
        if (context != null && !context.isBlank()) {
            userPrompt.append("Dữ liệu menu từ hệ thống:\n");
            userPrompt.append(context).append("\n\n");
        }
        userPrompt.append("Câu hỏi của khách hàng: ").append(message);

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", SYSTEM_PROMPT),
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
