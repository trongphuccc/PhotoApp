package thanhdnh.ueh.edu.article_app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;

public class ViewUserActivity extends AppCompatActivity {
  // Khóa của Intent extra chứa id user (MainActivity gửi sang)
  public static final String EXTRA_USER_ID = "id";

  ImageView iv_avatar;
  ProgressBar pb_avatar;
  TextView tv_avatar_progress;
  TextView tv_username, tv_user_id, tv_email, tv_tel, tv_hobby, tv_description;

  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private File avatarFile;   // file avatar đã tải về thư mục cache
  private int rotation = 0;  // góc xoay hiện tại: 0, 90, 180, 270

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_view_user);
    getSupportActionBar().hide();

    iv_avatar = findViewById(R.id.iv_avatar);
    pb_avatar = findViewById(R.id.pb_avatar);
    tv_avatar_progress = findViewById(R.id.tv_avatar_progress);
    tv_username = findViewById(R.id.tv_username);
    tv_user_id = findViewById(R.id.tv_user_id);
    tv_email = findViewById(R.id.tv_email);
    tv_tel = findViewById(R.id.tv_tel);
    tv_hobby = findViewById(R.id.tv_hobby);
    tv_description = findViewById(R.id.tv_description);

    int id = getIntent().getIntExtra(EXTRA_USER_ID, -1);
    UserProfile user = UserData.getUserFromId(id);
    if (user == null) {
      // Không tìm thấy (id sai, hoặc app bị hệ thống tắt nên dữ liệu tĩnh đã mất)
      Toast.makeText(this, R.string.error_user_not_found, Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    showInfo(user);

    // Xoay ảnh: mỗi lần chạm vào avatar thì xoay thêm 90° (Picasso .rotate)
    iv_avatar.setOnClickListener(v -> {
      if (avatarFile == null) return; // ảnh chưa tải xong thì chưa xoay
      rotation = (rotation + 90) % 360;
      showAvatar();
    });

    loadAvatar(user);
  }

  private void showInfo(UserProfile user) {
    tv_username.setText(orDash(user.getUsername()));
    tv_user_id.setText(getString(R.string.label_id, user.getId()));
    tv_email.setText(getString(R.string.label_email, orDash(user.getEmail())));
    tv_tel.setText(getString(R.string.label_tel, orDash(user.getTel())));

    // getHobbyList() tách chuỗi theo dấu phẩy; ghép lại để hiển thị gọn gàng
    List<String> hobbies = user.getHobbyList();
    String hobbyText = hobbies.isEmpty() ? getString(R.string.empty_value) : TextUtils.join(", ", hobbies);
    tv_hobby.setText(getString(R.string.label_hobby, hobbyText));

    // Có description thì hiện, không có thì ẩn hẳn (GONE)
    String description = user.getDescription();
    if (description == null || description.trim().isEmpty()) {
      tv_description.setVisibility(View.GONE);
    } else {
      tv_description.setText(description.trim());
      tv_description.setVisibility(View.VISIBLE);
    }
  }

  // Tải avatar về cache bằng downloadWithProgress; nếu đã có trong cache thì dùng luôn
  private void loadAvatar(UserProfile user) {
    String url = user.getAvatarUrl();
    if (url == null || url.trim().isEmpty()) {
      Downloader.hideProgress(pb_avatar, tv_avatar_progress); // không có ảnh -> giữ nền be
      return;
    }

    // Tên file gắn với id + URL: đổi avatar_url thì sẽ tải ảnh mới
    File avatarDir = new File(getCacheDir(), "avatars");
    String fileName = "avatar_" + user.getId() + "_" + Integer.toHexString(url.hashCode()) + ".jpg";
    File cached = new File(avatarDir, fileName);
    if (cached.exists() && cached.length() > 0) {
      Downloader.hideProgress(pb_avatar, tv_avatar_progress);
      avatarFile = cached;
      showAvatar();
      return;
    }

    Downloader.showProgress(pb_avatar, tv_avatar_progress, DownloadCallback.UNKNOWN_PROGRESS);
    Downloader.downloadWithProgress(url, mainHandler, avatarDir, fileName, new DownloadCallback() {
      @Override
      public void onProgress(int percent) {
        Downloader.showProgress(pb_avatar, tv_avatar_progress, percent);
      }

      @Override
      public void onSuccess(File file) {
        Downloader.hideProgress(pb_avatar, tv_avatar_progress);
        avatarFile = file;
        showAvatar();
      }

      @Override
      public void onError(Exception e) {
        Downloader.hideProgress(pb_avatar, tv_avatar_progress);
        String reason = Downloader.errorMessage(ViewUserActivity.this, e);
        Toast.makeText(ViewUserActivity.this, getString(R.string.error_avatar, reason), Toast.LENGTH_LONG).show();
      }
    });
  }

  // Dùng Picasso xử lý ảnh từ File đã tải: cắt vuông -> xoay -> bo góc
  private void showAvatar() {
    final File file = avatarFile;
    int size = getResources().getDimensionPixelSize(R.dimen.avatar_size);
    int radius = getResources().getDimensionPixelSize(R.dimen.avatar_corner_radius);

    Picasso.get()
        .load(file)
        .placeholder(R.drawable.avatar_placeholder)          // nền be trong lúc giải mã ảnh
        .error(R.drawable.avatar_placeholder)                // nền be nếu file ảnh hỏng
        .resize(size, size)                                  // cắt ảnh: đưa về khung vuông size x size...
        .centerCrop()                                        // ...giữ phần giữa, bỏ phần thừa
        .rotate(rotation)                                    // xoay ảnh theo góc hiện tại
        .transform(new RoundedCornersTransformation(radius)) // bo góc
        .into(iv_avatar, new Callback() {
          @Override
          public void onSuccess() {
          }

          @Override
          public void onError(Exception e) {
            // File trong cache bị hỏng -> xóa để lần mở sau tải lại
            file.delete();
            avatarFile = null;
          }
        });
  }

  private String orDash(String value) {
    return (value == null || value.trim().isEmpty()) ? getString(R.string.empty_value) : value;
  }
}
