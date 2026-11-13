package com.example.assistant.chat;

public final class ChatMessages {
  private ChatMessages() {}

  public static String error(String code) {
    return switch (code) {
      case "UNAUTHENTICATED" ->
          "Phiên đăng nhập không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.";
      case "FORBIDDEN" -> "Bạn không có quyền tra cứu thông tin này trong phạm vi đã chọn.";
      case "PASSWORD_CHANGE_REQUIRED" -> "Bạn cần đổi mật khẩu trước khi sử dụng trợ lý.";
      case "CONTEXT_CHANGED", "SESSION_CHANGED" ->
          "Tài khoản hoặc phạm vi đã thay đổi. Hãy mở hội thoại mới.";
      case "CONVERSATION_NOT_FOUND" -> "Không tìm thấy hội thoại trong phiên hiện tại.";
      case "CONVERSATION_BUSY" -> "Hội thoại đang xử lý một yêu cầu. Vui lòng chờ kết quả.";
      case "REQUEST_CONFLICT" -> "Mã yêu cầu đã được dùng cho nội dung khác.";
      case "INVALID_SELECTION" -> "Lựa chọn không còn hợp lệ. Hãy chọn lại từ danh sách hiện tại.";
      case "CHAT_DISABLED", "FEATURE_DISABLED", "MODEL_NOT_CONFIGURED" ->
          "Trợ lý hiện chưa được bật hoặc chưa hoàn tất cấu hình.";
      case "MODEL_RATE_LIMITED", "RATE_LIMITED", "CAPACITY_EXCEEDED" ->
          "Trợ lý đang đạt giới hạn sử dụng. Vui lòng thử lại sau.";
      case "MODEL_BUSY", "MODEL_COOLDOWN" ->
          "Dịch vụ AI đang bận hoặc tạm thời gián đoạn. Vui lòng chờ rồi thử lại.";
      case "MODEL_TIMEOUT" -> "Dịch vụ AI chưa phản hồi kịp. Vui lòng thử lại sau ít phút.";
      case "TURN_TIMEOUT" ->
          "Tra cứu mất quá nhiều thời gian. Vui lòng thu hẹp câu hỏi và thử lại.";
      case "INVALID_REQUEST",
          "INVALID_SCOPE",
          "INVALID_ARGUMENTS",
          "REQUEST_TOO_LARGE",
          "CONTEXT_TOO_LARGE" ->
          "Nội dung hoặc tham số chưa hợp lệ. Vui lòng kiểm tra và rút gọn yêu cầu.";
      case "NOT_FOUND" -> "Không tìm thấy dữ liệu phù hợp trong phạm vi được phép.";
      default -> "Hiện chưa lấy được kết quả tra cứu. Vui lòng thử lại sau.";
    };
  }

  public static String decision(String action, String reason) {
    if (action.equals("OUT_OF_SCOPE"))
      return "Tôi hỗ trợ tra cứu và sử dụng hệ thống Language Center. Bạn có thể hỏi về khóa học,"
          + " lớp, học viên, lịch, điểm danh, điểm số, học phí hoặc tư vấn trong phạm vi"
          + " được phân quyền.";
    if (action.equals("UNSUPPORTED_ACTION"))
      return "Trợ lý hiện chỉ hỗ trợ tra cứu. Bạn hãy thực hiện thao tác cập nhật tại trang nghiệp"
          + " vụ có quyền.";
    if (action.equals("UNSUPPORTED_CAPABILITY"))
      return "Yêu cầu liên quan đến trung tâm nhưng hiện chưa có công cụ tra cứu phù hợp. Vui lòng"
          + " mở trang nghiệp vụ hoặc hỏi cụ thể hơn.";
    return switch (reason) {
      case "NEED_STUDENT" -> "Bạn hãy cung cấp tên hoặc mã học viên để xác định đúng người.";
      case "NEED_CLASS" -> "Bạn hãy cung cấp mã hoặc chọn đúng lớp cần xem.";
      case "NEED_SESSION" -> "Bạn hãy chọn đúng buổi học cần xem điểm danh.";
      case "NEED_DATE" -> "Bạn muốn xem lịch vào ngày hoặc tuần nào?";
      case "NEED_RANGE" ->
          "Mức tiền tối thiểu đang lớn hơn mức tối đa. Bạn hãy xác nhận lại khoảng học phí muốn"
              + " tìm.";
      case "NEED_EXAM" -> "Bạn hãy cung cấp mã bài kiểm tra hoặc mở bài kiểm tra cần xem điểm.";
      case "LEAD_PRIORITY_CONFIRM" ->
          "Hệ thống chưa dự đoán khả năng đăng ký. Bạn có muốn xem khách hàng ưu tiên chăm sóc theo"
              + " lịch hẹn không?";
      case "NEED_SCOPE" ->
          "Bạn hãy chọn rõ phạm vi chi nhánh muốn tra cứu và bắt đầu hội thoại mới khi đổi phạm"
              + " vi.";
      default -> "Bạn hãy nêu rõ thông tin cần tra cứu trong hoạt động của trung tâm.";
    };
  }
}
