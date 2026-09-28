package thanhdnh.ueh.edu.article_app;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;

public class Downloader {
  public static String cached_file_path = "";

  // Dùng chung một OkHttpClient cho mọi lần tải (tiết kiệm kết nối và luồng)
  private static final OkHttpClient client = new OkHttpClient();

  // Lỗi HTTP (404, 500...): tách riêng để hiển thị thông báo rõ ràng cho người dùng
  public static class HttpStatusException extends IOException {
    public final int code;

    public HttpStatusException(int code) {
      super("HTTP " + code);
      this.code = code;
    }
  }

  public static File downloadFile(String url, File cached) {
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(url).build();

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) return null;
      String contentType = response.header("Content-Type", "");
      String extension = getExtensionFromMimeType(contentType);
      File file = File.createTempFile("downloaded_file", extension, cached);
      if (response.body() != null) {
        BufferedSink sink = Okio.buffer(Okio.sink(file));
        sink.writeAll(response.body().source());
        sink.close();
        return file;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  // Phiên bản trong bài giảng: tải ảnh rồi gắn vào ImageView.
  // Nay dùng lại bản có callback bên dưới để không lặp code.
  public static void downloadWithProgress(String inputurl, Handler mainHandler, Context context, File where2store, ProgressBar progressBar, ImageView imageView) {
    downloadWithProgress(inputurl, mainHandler, where2store, "downloaded_file", new DownloadCallback() {
      @Override
      public void onProgress(int percent) {
        progressBar.setIndeterminate(percent == UNKNOWN_PROGRESS);
        if (percent != UNKNOWN_PROGRESS) progressBar.setProgress(percent);
      }

      @Override
      public void onSuccess(File file) {
        cached_file_path = file.getAbsolutePath();
        imageView.setImageURI(Uri.fromFile(file));
        progressBar.setVisibility(ProgressBar.INVISIBLE);
      }

      @Override
      public void onError(Exception e) {
        progressBar.setVisibility(ProgressBar.INVISIBLE);
      }
    });
  }

  // Tải file bất đồng bộ, báo tiến trình (%) và kết quả qua callback.
  // - Việc tải chạy trên luồng nền của OkHttp (enqueue), KHÔNG chạy trên UI thread.
  // - Mọi lời gọi callback đều được post qua mainHandler -> chạy trên UI thread.
  // - File lưu tại where2store/fileName (thường là thư mục cache của app).
  public static void downloadWithProgress(String inputurl, Handler mainHandler, File where2store, String fileName, DownloadCallback callback) {
    Request request;
    try {
      request = new Request.Builder().url(inputurl).build();
    } catch (IllegalArgumentException e) {
      // URL sai định dạng (vd. thiếu "https://") -> báo lỗi thay vì làm crash app
      mainHandler.post(() -> callback.onError(e));
      return;
    }

    client.newCall(request).enqueue(new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        // Mất mạng, không phân giải được tên miền, hết thời gian chờ...
        mainHandler.post(() -> callback.onError(e));
      }

      @Override
      public void onResponse(Call call, Response response) {
        // Hàm này chạy trên luồng nền của OkHttp
        try (ResponseBody body = response.body()) {
          if (!response.isSuccessful()) throw new HttpStatusException(response.code());
          if (body == null) throw new IOException("Response body is empty");

          // -1 nếu server không gửi Content-Length (vd. dữ liệu nén gzip)
          long totalBytes = body.contentLength();
          if (totalBytes <= 0) {
            mainHandler.post(() -> callback.onProgress(DownloadCallback.UNKNOWN_PROGRESS));
          } else {
            mainHandler.post(() -> callback.onProgress(0));
          }

          if (!where2store.exists()) where2store.mkdirs();
          // Ghi ra file tạm trước, tải xong mới đổi tên -> không bao giờ để lại file tải dở
          File partFile = new File(where2store, fileName + ".part");
          File outFile = new File(where2store, fileName);

          try (InputStream inputStream = body.byteStream();
               OutputStream outputStream = new FileOutputStream(partFile)) {
            byte[] buffer = new byte[8192];
            long downloadedBytes = 0;
            int lastPercent = 0;
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
              outputStream.write(buffer, 0, bytesRead);
              downloadedBytes += bytesRead;
              if (totalBytes > 0) {
                int percent = (int) ((downloadedBytes * 100) / totalBytes);
                // Chỉ báo khi % thay đổi để không gửi quá nhiều việc lên UI thread
                if (percent != lastPercent) {
                  lastPercent = percent;
                  mainHandler.post(() -> callback.onProgress(percent));
                }
              }
            }
            outputStream.flush();
          }

          if (outFile.exists()) outFile.delete();
          if (!partFile.renameTo(outFile)) throw new IOException("Cannot save " + outFile);

          mainHandler.post(() -> callback.onSuccess(outFile));
        } catch (IOException e) {
          mainHandler.post(() -> callback.onError(e));
        }
      }
    });
  }

  // Cập nhật ProgressBar ngang + dòng chữ phần trăm. Phải gọi trên UI thread.
  public static void showProgress(ProgressBar progressBar, TextView tvPercent, int percent) {
    progressBar.setVisibility(View.VISIBLE);
    tvPercent.setVisibility(View.VISIBLE);
    if (percent == DownloadCallback.UNKNOWN_PROGRESS) {
      // Không biết tổng dung lượng -> thanh chạy liên tục, không hiện % giả
      progressBar.setIndeterminate(true);
      tvPercent.setText(R.string.loading);
    } else {
      progressBar.setIndeterminate(false);
      progressBar.setProgress(percent);
      tvPercent.setText(tvPercent.getContext().getString(R.string.loading_percent, percent));
    }
  }

  public static void hideProgress(ProgressBar progressBar, TextView tvPercent) {
    progressBar.setVisibility(View.GONE);
    tvPercent.setVisibility(View.GONE);
  }

  // Chuyển Exception thành câu thông báo dễ hiểu để hiện Toast
  public static String errorMessage(Context context, Exception e) {
    if (e instanceof HttpStatusException) {
      return context.getString(R.string.error_http, ((HttpStatusException) e).code);
    }
    if (e instanceof UnknownHostException || e instanceof SocketException || e instanceof SocketTimeoutException) {
      return context.getString(R.string.error_no_network);
    }
    return context.getString(R.string.error_download);
  }

  private static String getExtensionFromMimeType(String mimeType) {
    Map<String, String> mimeMap = new HashMap<>();
    mimeMap.put("image/jpeg", ".jpg");
    mimeMap.put("image/png", ".png");
    mimeMap.put("application/json", ".json");
    return mimeMap.getOrDefault(mimeType, "");
  }
}
