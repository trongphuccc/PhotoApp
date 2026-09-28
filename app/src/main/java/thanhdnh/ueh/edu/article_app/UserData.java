package thanhdnh.ueh.edu.article_app;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserData {
  // Danh sách user dùng chung cho màn hình Home và Detail
  public static UserList data;

  private static final String JSON_FILE_NAME = "users.json";
  // Luồng nền để đọc file và parse JSON (không làm trên UI thread)
  private static final ExecutorService executor = Executors.newSingleThreadExecutor();

  private final Activity activity;
  private final GridView gridview;
  private final ProgressBar progressBar;
  private final TextView tvProgress;
  // Handler gắn với UI thread: dùng để đưa kết quả từ luồng nền về cập nhật View
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  public UserData(Activity activity, GridView gridview, ProgressBar progressBar, TextView tvProgress) {
    this.activity = activity;
    this.gridview = gridview;
    this.progressBar = progressBar;
    this.tvProgress = tvProgress;
  }

  // Tìm user theo id; trả về null nếu chưa có dữ liệu hoặc không tìm thấy
  public static UserProfile getUserFromId(int id) {
    if (data == null || data.getUsers() == null) return null;
    for (UserProfile user : data.getUsers())
      if (user.getId() == id)
        return user;
    return null;
  }

  // Bước 1: tải file JSON về cache, vừa tải vừa cập nhật ProgressBar
  public void loadData(String url) {
    // Chưa nhận được phản hồi nên chưa biết dung lượng -> hiện dạng indeterminate
    Downloader.showProgress(progressBar, tvProgress, DownloadCallback.UNKNOWN_PROGRESS);

    Downloader.downloadWithProgress(url, mainHandler, activity.getCacheDir(), JSON_FILE_NAME, new DownloadCallback() {
      @Override
      public void onProgress(int percent) {
        Downloader.showProgress(progressBar, tvProgress, percent);
      }

      @Override
      public void onSuccess(File file) {
        parseAndShow(file);
      }

      @Override
      public void onError(Exception e) {
        Downloader.hideProgress(progressBar, tvProgress);
        Toast.makeText(activity, Downloader.errorMessage(activity, e), Toast.LENGTH_LONG).show();
      }
    });
  }

  // Bước 2: tải xong mới readText -> Gson -> UserList (trên luồng nền),
  // rồi quay về UI thread để gắn adapter và ẩn ProgressBar.
  private void parseAndShow(File file) {
    executor.execute(() -> {
      UserList users = null;
      int errorMessage = 0;
      try {
        // Chỉ map các thuộc tính có @Expose
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        users = gson.fromJson(readText(file), UserList.class);
        if (users == null || users.getUsers() == null || users.getUsers().isEmpty())
          errorMessage = R.string.error_empty_data;   // file rỗng hoặc không có khóa "users"
      } catch (JsonParseException e) {
        errorMessage = R.string.error_json;           // sai cú pháp JSON hoặc sai kiểu dữ liệu
      } catch (IOException e) {
        errorMessage = R.string.error_download;       // không đọc được file đã tải
      }

      final UserList result = users;
      final int error = errorMessage;
      mainHandler.post(() -> {
        Downloader.hideProgress(progressBar, tvProgress);
        if (error != 0) {
          Toast.makeText(activity, error, Toast.LENGTH_LONG).show();
          return;
        }
        data = result;
        gridview.setAdapter(new UserAdapter(data.getUsers(), activity));
      });
    });
  }

  // Đọc toàn bộ nội dung file văn bản (UTF-8 để giữ đúng dấu tiếng Việt)
  public static String readText(File file) throws IOException {
    StringBuilder builder = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        builder.append(line).append('\n');
      }
    }
    return builder.toString();
  }
}
