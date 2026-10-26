# Công cụ kiểm thử bổ sung (ngoài `mvn package`)

Các công cụ này **không** chạy trong `mvn package` vì cần màn hình, chuột, Chrome và Internet.

| File | Dùng để | Cách chạy |
|---|---|---|
| `kiem-tra-form.py` | Kiểm tra mỗi file `.form` khớp với vùng `GEN` trong `.java` (component, bố cục, biến, hàm sự kiện). Thay cho việc mở NetBeans xem có lỗi không. | `python kiemthu\kiem-tra-form.py` |
| `sinh-form.py` | Sinh `MainFrame.form` và vùng `GEN-BEGIN/GEN-END` của `MainFrame.java` từ một bảng mô tả giao diện (4 thẻ, ~70 component). Chỉ cần chạy lại khi muốn đổi bố cục bằng mã; phần logic ngoài vùng GEN không bị đụng. Nếu đã sửa bố cục trong NetBeans thì **đừng** chạy lại (sẽ ghi đè). | `python kiemthu\sinh-form.py` |
| `GuiRobot.java`, `Scenarios.java` | Kiểm thử giao diện bằng **chuột và bàn phím thật** (java.awt.Robot): bật/tắt proxy, chọn thẻ, nhập sai, sửa danh sách chặn, lọc nhật ký, thẻ Thống kê, mở và dùng 4 hộp thoại, đổi cỡ cửa sổ, đóng cửa sổ xuống khay. In `[DAT]`/`[LOI]` từng bước và lưu ảnh chụp. | Xem bên dưới |
| `thu-tuong-thich.ps1` | Chrome thật đi qua proxy tới Wikipedia, YouTube, Facebook, Zalo, Google Meet, Google, tải file 10 MB, HTTP thường; chạy 2 lần: không giải mã và có giải mã HTTPS. In bảng đạt/lỗi. | `powershell -ExecutionPolicy Bypass -File kiemthu\thu-tuong-thich.ps1` |
| `DoTai.java` | Đo độ trễ có/không có proxy với nhiều luồng, RAM và số luồng theo thời gian. Tham số: số luồng, số yêu cầu mỗi luồng, số phút chạy liên tục. | `java -cp "target\littleproxy-1.1.3-SNAPSHOT-littleproxy-shade.jar;." DoTai 20 100 10` (sau khi biên dịch `DoTai.java`) |
| `thu-trinh-duyet.ps1` | Thử bằng Chrome thật (hồ sơ riêng, chỉ tin chứng chỉ gốc của proxy, không đụng cài đặt Windows): HTTP bị chặn, HTTPS giải mã, quy tắc đường dẫn và từ khóa. | `powershell -ExecutionPolicy Bypass -File kiemthu\thu-trinh-duyet.ps1` |

## Chạy kiểm thử giao diện

Đóng mọi cửa sổ Proxy Server đang mở, rồi đứng ở thư mục dự án:

```
mvn package
cd kiemthu
javac -encoding UTF-8 -cp ..\target\littleproxy-1.1.3-SNAPSHOT-littleproxy-shade.jar GuiRobot.java Scenarios.java
java -Dstdout.encoding=UTF-8 -cp "..\target\littleproxy-1.1.3-SNAPSHOT-littleproxy-shade.jar;." GuiRobot %TEMP%\proxyapp-gui
```

Trong lúc chạy (khoảng 1 đến 2 phút) chương trình tự điều khiển chuột và bàn phím, nên đừng đụng vào máy. Dữ liệu thử nằm trong thư mục truyền vào (`%TEMP%\proxyapp-gui`), không đụng cấu hình thật của bạn. Kết thúc in `TONG KET: N dat, M loi`. Ảnh chụp từng hộp thoại ở thư mục con `shots`.

Các bước kiểm thử không làm (vì đổi cài đặt hệ thống): nút "Bật proxy hệ thống Windows" bằng chuột, ô "Tự mở cùng Windows", cài chứng chỉ vào Windows.
