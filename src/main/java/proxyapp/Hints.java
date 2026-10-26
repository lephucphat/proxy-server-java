package proxyapp;

public class Hints {

    public static final String[] LOG_COLUMNS = {
        "Thời điểm proxy nhận yêu cầu.",
        "Địa chỉ IP của máy gửi yêu cầu. 127.0.0.1 là chính máy đang chạy proxy.",
        "Tài khoản đã đăng nhập vào proxy. Để trống nếu chưa bật đăng nhập.",
        "GET = lấy dữ liệu (mở trang). POST = gửi dữ liệu đi. CONNECT = mở đường hầm HTTPS, lúc này chỉ thấy tên miền.",
        "Trang web mà yêu cầu này gửi tới.",
        "Đường link đầy đủ. Trang HTTPS chỉ hiện tên-miền:443 nếu chưa bật giải mã HTTPS.",
        "Kết quả: 200 thành công, 304 dùng bản đã lưu, 403 bị chặn, 407 cần đăng nhập, 429 bị khóa tạm, 502 lỗi máy chủ.",
        "Số byte dữ liệu trả về cho máy khách.",
        "Thời gian xử lý, tính bằng mili giây (1000 ms = 1 giây).",
        "Có = proxy đã chặn yêu cầu này."
    };

    public static final String[] STATS_COLUMNS = {
        "Địa chỉ máy hoặc tên người dùng.",
        "Số yêu cầu đã gửi qua proxy.",
        "Số yêu cầu bị proxy chặn.",
        "Số lần nhập sai mật khẩu đăng nhập proxy.",
        "Tổng dữ liệu proxy đã tải về cho máy này.",
        "Tổng dữ liệu máy này đã gửi đi qua proxy."
    };

    public static final String REQUESTS = "Tổng số yêu cầu proxy đã nhận. Mỗi ảnh, đoạn mã hay trang trình duyệt tải về tính một yêu cầu.";
    public static final String BLOCKED = "Số yêu cầu bị proxy chặn (trang trong danh sách chặn, đường dẫn bị cấm, ngoài giờ cho phép...).";
    public static final String CONNECTIONS = "Số kết nối đang mở tới proxy ngay lúc này.";
    public static final String DOWNLOAD = "Tổng dữ liệu proxy đã tải về giúp các máy khách.";
    public static final String UPLOAD = "Tổng dữ liệu các máy khách đã gửi đi qua proxy.";
    public static final String WRONG_PASSWORD = "Số lần nhập sai mật khẩu đăng nhập proxy. Sai nhiều lần thì địa chỉ đó bị khóa tạm.";

    public static final String PORT = "Số cổng proxy lắng nghe. Trình duyệt phải đặt đúng số này. Mặc định 8080.";
    public static final String LAN = "Cho điện thoại, laptop khác trong cùng Wi-Fi dùng proxy này. Khi bật phải bật thêm đăng nhập.";
    public static final String AUTH = "Bắt người dùng nhập tài khoản và mật khẩu mới được dùng proxy.";
    public static final String USER = "Tên tài khoản chính của proxy.";
    public static final String PASSWORD = "Để trống nghĩa là giữ mật khẩu cũ. Mật khẩu được lưu dạng mã hóa.";
    public static final String BLOCK_LIST = "Mỗi dòng một trang, ví dụ youtube.com. Chặn cả trang và các tên miền con, kể cả khi bạn dán link dài.";
    public static final String SAVE_BLOCK = "Lưu danh sách. Có hiệu lực ngay, không cần tắt proxy.";
    public static final String FILTER = "Gõ tên miền, địa chỉ hoặc tên người dùng để chỉ xem các dòng khớp.";
    public static final String BLOCKED_ONLY = "Chỉ hiện các dòng proxy đã chặn.";
    public static final String CLEAR_ALERTS = "Xóa các dòng trong khung cảnh báo.";
    public static final String RESET_STATS = "Đưa mọi số liệu thống kê và biểu đồ về 0.";
    public static final String CHART = "Đường xanh là dữ liệu tải về, đường cam là dữ liệu gửi đi, trong 5 phút gần nhất.";
    public static final String TOP_HOSTS = "Đếm số yêu cầu, không phải số lần mở trang. Một trang thường gửi hàng chục yêu cầu.";
    public static final String ALERTS = "Cảnh báo khi một máy bị chặn nhiều lần trong thời gian ngắn, khi địa chỉ bị khóa vì sai mật khẩu, hoặc khi máy lạ cố dùng proxy.";

    public static String logColumn(int index) {
        return index >= 0 && index < LOG_COLUMNS.length ? LOG_COLUMNS[index] : null;
    }

    public static String statsColumn(int index) {
        return index >= 0 && index < STATS_COLUMNS.length ? STATS_COLUMNS[index] : null;
    }

    public static String tabsText() {
        return "TỔNG QUAN\n"
                + "Cài đặt cơ bản của proxy: cổng, cho máy khác trong mạng LAN dùng, đăng nhập, giới hạn tốc độ, chạy nền và tự mở cùng Windows.\n\n"
                + "QUY TẮC\n"
                + "Quyết định trang nào bị chặn. Danh sách chặn chặn cả trang. Ba nút bên phải mở thêm: danh sách chặn công khai, chỉ cho phép và lịch chặn theo giờ, giải mã HTTPS để chặn theo đường dẫn hoặc từ khóa.\n\n"
                + "NHẬT KÝ\n"
                + "Mỗi yêu cầu đi qua proxy hiện một dòng: ai gửi, tới trang nào, kết quả ra sao, có bị chặn không. Rê chuột vào tiêu đề cột để xem nghĩa. Có ô lọc, chuột phải để sao chép địa chỉ, nút xuất ra file CSV.\n\n"
                + "THỐNG KÊ\n"
                + "Biểu đồ dữ liệu 5 phút gần nhất, bảng theo từng máy và từng người dùng, các trang được truy cập nhiều nhất, và các cảnh báo.\n\n"
                + "CẢNH BÁO\n"
                + ALERTS + " Ngưỡng mặc định: 20 lần bị chặn trong 5 phút.";
    }

    public static String codesText() {
        return "Mã kết quả trong cột Mã của Nhật ký:\n\n"
                + "200  Thành công, trang trả về bình thường.\n"
                + "204  Thành công, không có nội dung.\n"
                + "304  Không đổi, trình duyệt dùng bản đã lưu.\n"
                + "403  Bị chặn bởi proxy (danh sách chặn, quy tắc, lịch, hoặc không được phép).\n"
                + "407  Cần đăng nhập vào proxy.\n"
                + "429  Địa chỉ bị khóa tạm vì nhập sai mật khẩu nhiều lần.\n"
                + "499  Trình duyệt tự đóng kết nối trước khi xong (bình thường).\n"
                + "502  Proxy không nối được tới trang web hoặc chứng chỉ trang không hợp lệ.\n"
                + "504  Trang web trả lời quá chậm.\n\n"
                + "Cột Loại:\n"
                + "GET      Lấy dữ liệu (mở trang, tải ảnh).\n"
                + "POST     Gửi dữ liệu đi (đăng nhập, gửi biểu mẫu).\n"
                + "CONNECT  Mở đường hầm cho trang HTTPS. Chưa bật giải mã thì chỉ thấy tên miền, không thấy đường dẫn.";
    }
}
