# Proxy Server

Ứng dụng proxy HTTP/HTTPS bằng Java, có giao diện Swing (NetBeans form), chạy trên Windows. Đồ án cuối kỳ môn Lập trình mạng, chủ đề Network security.

## Tính năng

- Chặn tên miền (kèm tên miền con), nhập danh sách chặn công khai từ URL hoặc file, tự cập nhật theo giờ
- Chế độ chỉ cho phép, lịch chặn theo giờ, quy tắc chặn theo đường dẫn và từ khóa
- Đăng nhập proxy, nhiều tài khoản với luật riêng, khóa IP khi nhập sai mật khẩu, giới hạn IP máy được dùng
- Dùng chung trong mạng LAN (bắt buộc đăng nhập, không cho máy khách vào mạng nội bộ qua proxy)
- Giải mã HTTPS (MITM) bằng CA riêng, có danh sách trang không giải mã
- Nhật ký truy cập có lọc và xuất CSV, thống kê theo máy khách và người dùng, biểu đồ lưu lượng, cảnh báo
- Bật/tắt proxy hệ thống Windows, chạy nền ở khay hệ thống, tự mở cùng Windows
- Chạy không giao diện bằng `ConsoleMain`

## Yêu cầu

JDK 17 trở lên (biên dịch ra Java 8), Maven 3.6 trở lên.

## Build và chạy

```
mvn clean package
java -jar target\littleproxy-1.1.3-SNAPSHOT-littleproxy-shade.jar
```

Chạy dòng lệnh:

```
java -cp target\littleproxy-1.1.3-SNAPSHOT-littleproxy-shade.jar proxyapp.ConsoleMain --port 8080
```

Khôi phục proxy hệ thống Windows nếu ứng dụng bị tắt đột ngột:

```
java -cp target\littleproxy-1.1.3-SNAPSHOT-littleproxy-shade.jar proxyapp.ConsoleMain --restore-system-proxy
```

Hướng dẫn sử dụng chi tiết: [HUONG_DAN_SU_DUNG.md](HUONG_DAN_SU_DUNG.md).

## Kiểm thử

`mvn clean package` chạy toàn bộ test JUnit trong `src/test/java/proxyapp`. Công cụ kiểm thử bổ sung (giao diện bằng chuột thật, Chrome thật, tương thích trang web, đo tải) nằm trong thư mục `kiemthu`, xem [kiemthu/README.md](kiemthu/README.md).

## Cấu trúc

- `src/main/java/proxyapp`: mã ứng dụng (giao diện, cấu hình, bộ lọc, thống kê, giải mã HTTPS)
- `src/main/java/org/littleshoot/proxy`: lõi proxy
- `src/test/java/proxyapp`: test tự động
- `kiemthu`: công cụ kiểm thử bổ sung

## Giấy phép và ghi công

Lõi proxy dựa trên [LittleProxy](https://github.com/adamfisk/LittleProxy) (Apache License 2.0), đã được chỉnh sửa. Giấy phép nằm trong [LICENSE.txt](LICENSE.txt), thông báo bản quyền trong [COPYRIGHT.txt](COPYRIGHT.txt).
