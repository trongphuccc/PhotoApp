package thanhdnh.ueh.edu.article_app;

import java.io.File;

// Callback báo kết quả tải file của Downloader.downloadWithProgress(...).
// Cả 3 hàm đều được gọi trên UI thread (qua Handler), nên có thể cập nhật View trực tiếp.
public interface DownloadCallback {
  // Giá trị percent đặc biệt: server không gửi Content-Length -> không biết tổng dung lượng
  int UNKNOWN_PROGRESS = -1;

  // percent: 0..100, hoặc UNKNOWN_PROGRESS
  void onProgress(int percent);

  // Tải xong, file đã được lưu vào thư mục cache
  void onSuccess(File file);

  // Lỗi mạng, lỗi HTTP (404, 500...) hoặc lỗi ghi file
  void onError(Exception e);
}
