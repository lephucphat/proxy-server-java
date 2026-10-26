# Hướng dẫn sử dụng Proxy Server

Ứng dụng proxy HTTP/HTTPS viết bằng Java (dựa trên LittleProxy), có giao diện Swing, chạy trên Windows. Đồ án cuối kỳ môn Lập trình mạng, chủ đề Network security.

## 1. Chạy chương trình

**Bản đóng gói (không cần cài Java):** giải nén `ProxyServer-windows.zip`, mở `ProxyServer\ProxyServer.exe`. Cùng thư mục có `ProxyConsole.exe` để chạy không giao diện (xem mục 10).

**Từ NetBeans hoặc file .jar:**

```
mvn package
java -jar target\littleproxy-1.1.3-SNAPSHOT-littleproxy-shade.jar
```

**Dữ liệu lưu ở đâu:** bản đóng gói lưu ở `%APPDATA%\ProxyServer`. Chạy từ file .jar thì lưu ở thư mục đang đứng. Muốn đổi thì thêm `--home "đường dẫn"` khi chạy. Các file ở đó: `proxy.properties` (cấu hình), `blocklist.txt`, `allowlist.txt`, `schedule.txt`, `url-rules.txt`, `mitm-bypass.txt`, `users.txt` và thư mục `users`, `proxy-ca.p12` (khóa riêng của CA, giữ kín).

Chỉ mở được một bản. Mở bản thứ hai thì bản đang chạy hiện cửa sổ lên.

## 2. Bắt đầu nhanh

Lần mở đầu tiên có hộp thoại **Bắt đầu nhanh** nhắc lại 3 bước này.

1. Bấm **Bật proxy** (cổng mặc định 8080).
2. Đặt proxy cho trình duyệt: `127.0.0.1`, cổng `8080`. Hoặc bấm **Bật proxy hệ thống Windows** để Windows tự dùng proxy này (bấm lại để trả về như cũ; app cũng tự trả lại khi tắt proxy hoặc thoát).
3. Mở web. Thẻ **Nhật ký** hiện từng yêu cầu; các trang quảng cáo trong danh sách chặn mặc định trả trang "Truy cập bị chặn".

**Các thẻ của cửa sổ chính** (cửa sổ co giãn được, nút Bật/Tắt proxy và thanh trạng thái luôn hiện):
- **Tổng quan**: cổng, LAN, giới hạn tốc độ, đăng nhập, IP máy được phép, đóng xuống khay, tự bật proxy, tự mở cùng Windows.
- **Quy tắc**: danh sách chặn và 3 nút mở hộp thoại (danh sách công khai, chỉ cho phép và lịch, giải mã HTTPS).
- **Nhật ký**: bảng truy cập. Ô **Lọc** lọc theo tên miền, máy khách, người dùng hoặc địa chỉ; tích **Chỉ hiện dòng bị chặn** để xem riêng các dòng bị chặn. Có nút **Xóa nhật ký** và **Xuất CSV**.
- **Thống kê**: biểu đồ, bảng theo máy khách và người dùng, cảnh báo, tên miền truy cập nhiều nhất (mục 6).

## 3. Cho máy khác hoặc điện thoại dùng (LAN)

1. Tích **Cho phép máy khác trong mạng LAN dùng**. Khi đó **bắt buộc** tích **Yêu cầu đăng nhập**, đặt tài khoản và mật khẩu (mật khẩu được lưu dạng băm).
2. Tùy chọn: ô **IP máy được phép** để chỉ cho một số máy, ví dụ `192.168.1.0/24, 192.168.1.50`. Máy ngoài danh sách bị từ chối.
3. Bấm **Bật proxy**. Windows hỏi tường lửa thì cho phép trên mạng riêng.
4. Khi proxy đang chạy với LAN bật, dòng gợi ý ở thẻ **Tổng quan** hiện sẵn địa chỉ và cổng cho điện thoại (cũng xem được bằng lệnh `ipconfig`).
   - Android: Cài đặt > Wi-Fi > nhấn giữ mạng đang dùng > Sửa mạng > Tùy chọn nâng cao > Proxy: Thủ công, nhập IP máy chạy proxy và cổng.
   - iPhone: Cài đặt > Wi-Fi > bấm (i) cạnh mạng đang dùng > Định cấu hình proxy > Thủ công, nhập IP, cổng, bật Xác thực và nhập tài khoản, mật khẩu.
   - Nên đặt IP cố định cho máy chạy proxy trong router để địa chỉ không đổi.
5. Máy khách trong LAN không vào được `127.0.0.1` hay mạng nội bộ của máy chạy proxy qua proxy.
6. Sai mật khẩu 5 lần thì địa chỉ đó bị khóa 5 phút.

## 4. Chặn và cho phép

- **Danh sách chặn** (thẻ **Quy tắc**, khung bên trái): mỗi dòng một tên miền, chặn luôn tên miền con, `#` là ghi chú. Bấm **Lưu danh sách chặn**, có hiệu lực ngay.
- **Danh sách chặn công khai...**: nhập danh sách quảng cáo, theo dõi, mã độc từ địa chỉ http/https hoặc file. Nút **Điền nguồn gợi ý** điền sẵn hai nguồn. Có thể tự cập nhật theo giờ.
- **Chỉ cho phép & lịch chặn theo giờ...**:
  - Chế độ chỉ cho phép: chỉ các tên miền trong danh sách cho phép được vào.
  - Lịch: mỗi dòng `tên-miền ngày giờ`, ví dụ `facebook.com T2-T6 07:00-17:00`. Dòng `*` chặn mọi trang trong khung giờ, trừ danh sách cho phép.
- **Quy tắc đường dẫn và từ khóa** nằm trong hộp thoại **Giải mã HTTPS...**: `youtube.com path /shorts`, `* keyword casino`. Với HTTPS chỉ thấy đường dẫn khi bật giải mã (mục 7).
- Thứ tự kiểm tra: danh sách chặn, quy tắc đường dẫn/từ khóa, lịch, chế độ chỉ cho phép.

## 5. Nhiều tài khoản và luật riêng

Bấm **Tài khoản khác...** để thêm tài khoản phụ ngoài tài khoản chính. Mỗi tài khoản có thể bật **Dùng luật riêng**: danh sách cho phép, lịch và chế độ chỉ cho phép riêng thay cho luật chung. Nhật ký và thống kê ghi tên người dùng.

## 6. Thống kê và cảnh báo

Thẻ **Thống kê** hiện biểu đồ lưu lượng 5 phút gần nhất, bảng theo máy khách và theo người dùng, danh sách cảnh báo (bị chặn quá nhiều, địa chỉ bị khóa, máy lạ cố dùng proxy) và tên miền truy cập nhiều nhất. Nút **Đặt lại thống kê** xóa số liệu và biểu đồ. Cảnh báo cũng hiện bằng bóng báo ở khay hệ thống. Ngưỡng chỉnh trong `proxy.properties` (`alertBlocked`, `alertMinutes`; `alertBlocked=0` là tắt).

## 7. Giải mã HTTPS (tùy chọn, cần cân nhắc)

Mặc định proxy chỉ thấy tên miền của trang HTTPS. Bật giải mã thì thấy cả đường dẫn để ghi nhật ký và chặn theo quy tắc.

**Chỉ dùng trên thiết bị của chính bạn hoặc khi người dùng đã đồng ý.**

1. Bấm **Giải mã HTTPS...**, bấm **Tạo / xuất chứng chỉ gốc...** để có `proxy-ca.crt`.
2. Cài chứng chỉ này làm "Trusted Root Certification Authority" trên máy khách (hộp thoại có hướng dẫn từng bước cho Windows, Firefox, điện thoại và cách gỡ).
3. Tích **Bật giải mã HTTPS**, bấm **Lưu**, rồi tắt và bật lại proxy.
4. Ngân hàng, ví điện tử và ứng dụng ghim chứng chỉ sẽ lỗi khi bị giải mã: thêm vào ô **Trang KHÔNG giải mã**.

Lưu ý: ai có file `proxy-ca.p12` thì giả mạo được mọi trang trên máy đã cài chứng chỉ. Công cụ dùng bộ kiểm tra thu hồi chứng chỉ của Windows (như `curl`) cần thêm `--ssl-no-revoke`; Chrome và Edge thì không cần.

## 8. Chạy nền và tự khởi động

- **Đóng cửa sổ thì chạy nền ở khay**: khi proxy đang chạy, bấm X chỉ ẩn cửa sổ; bấm đúp biểu tượng "P" ở khay để mở lại; menu chuột phải có Bật/Tắt proxy và Thoát.
- **Tự bật proxy khi mở app** và **Tự mở cùng Windows** (chỉ dùng được khi chạy từ file .exe hoặc .jar): mở máy lên là proxy tự chạy ẩn dưới khay.

## 9. Giới hạn tốc độ

Ô **Giới hạn tốc độ (KB/s)** áp dụng chung cho cả proxy (không theo từng người).

## 10. Dòng lệnh

```
ProxyConsole.exe --port 8080 --user admin --pass 123456 --lan --allow 192.168.1.0/24
```

Tham số: `--port`, `--user`, `--pass`, `--lan`, `--allow`, `--public nguon1;nguon2`, `--update-hours`, `--allow-only`, `--mitm`, `--system-proxy`, `--limit`, `--blocklist`, `--logfile`, `--home`.

Nếu ứng dụng bị tắt đột ngột (kết thúc bằng Task Manager, mất điện) khi đang bật proxy hệ thống Windows và trình duyệt không vào được mạng, chạy:

```
ProxyConsole.exe --restore-system-proxy
```

Hoặc tắt proxy thủ công trong Cài đặt Windows > Mạng và Internet > Proxy. Mở lại ứng dụng cũng tự khôi phục.

## 11. Giới hạn và an toàn

- Proxy chỉ lọc được các ứng dụng chịu đi qua nó. Người dùng đổi cài đặt proxy/Wi-Fi hoặc dùng VPN, dữ liệu di động thì tránh được; muốn bắt buộc phải chặn ở router.
- Chỉ dùng trong mạng tin cậy, không đưa ra Internet: thư viện mạng (Netty 4.0.44, log4j 1.x) đã ngừng cập nhật vá lỗi.
- Giải mã HTTPS chỉ dùng TLS 1.2 (Netty cũ lỗi với TLS 1.3 trên JDK mới). Trang chỉ chạy TLS 1.3 sẽ trả lỗi 502.
- Giới hạn tốc độ và quy tắc đường dẫn/từ khóa dùng chung, chưa theo từng người. Thống kê không lưu xuống đĩa.
- Mật khẩu của file `proxy-ca.p12` cố định trong mã nguồn nên chỉ che bớt; hãy bảo vệ file bằng quyền thư mục Windows.
- Khi xuất nhật ký CSV, ô nào bắt đầu bằng `=`, `+`, `-`, `@` được thêm dấu `'` phía trước để Excel không chạy nhầm thành công thức.
- Các bước kiểm thử bổ sung (giao diện bằng chuột thật, Chrome thật) nằm trong thư mục `kiemthu`, xem `kiemthu\README.md`.

## 12. Gặp lỗi thì xem

| Hiện tượng | Cách xử lý |
|---|---|
| "Cổng N đang được chương trình khác dùng" | Đổi sang cổng khác ở thẻ Tổng quan, hoặc tắt chương trình đang chiếm cổng. |
| Trình duyệt hỏi tài khoản liên tục | Nhập đúng tài khoản và mật khẩu của proxy (mục 3). |
| 429 "Tạm khóa" | Sai mật khẩu nhiều lần, chờ hết thời gian khóa. |
| Mã 499 trong nhật ký | Trình duyệt tự đóng kết nối trước khi xong (Chrome hay mở sẵn kết nối rồi bỏ). Bình thường, không phải lỗi. |
| 403 "Truy cập bị chặn" | Trang nằm trong danh sách chặn, lịch hoặc quy tắc; xem cột Mã và Chặn trong nhật ký. |
| 502 với trang HTTPS | Chứng chỉ máy chủ không hợp lệ, hoặc trang chỉ chạy TLS 1.3. Thêm vào ô không giải mã nếu cần. |
| Trình duyệt báo chứng chỉ không tin cậy | Chưa cài `proxy-ca.crt` vào máy khách (mục 7). |
