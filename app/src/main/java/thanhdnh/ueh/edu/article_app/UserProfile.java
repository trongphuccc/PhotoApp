package thanhdnh.ueh.edu.article_app;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

// Một user trong file JSON.
// @SerializedName: tên khóa trong JSON; @Expose: cho phép Gson đọc/ghi thuộc tính này
// (vì khi parse ta dùng excludeFieldsWithoutExposeAnnotation()).
public class UserProfile {
  @SerializedName("id")
  @Expose
  private int id;

  @SerializedName("username")
  @Expose
  private String username;

  @SerializedName("email")
  @Expose
  private String email;

  // Lưu dạng String để không mất số 0 ở đầu (vd. "0901234567")
  @SerializedName("tel")
  @Expose
  private String tel;

  @SerializedName("description")
  @Expose
  private String description;

  // Đường dẫn ảnh đại diện trên Internet
  @SerializedName("avatar_url")
  @Expose
  private String avatar_url;

  // Các sở thích nằm chung một chuỗi, phân cách bằng dấu phẩy: "Đọc sách, Bơi lội, Chơi guitar"
  @SerializedName("hobby")
  @Expose
  private String hobby;

  public UserProfile() {
  }

  public UserProfile(int id, String username, String email, String tel, String description, String avatar_url, String hobby) {
    this.id = id;
    this.username = username;
    this.email = email;
    this.tel = tel;
    this.description = description;
    this.avatar_url = avatar_url;
    this.hobby = hobby;
  }

  // Tách chuỗi hobby theo dấu phẩy, bỏ khoảng trắng thừa và phần tử rỗng
  public List<String> getHobbyList() {
    List<String> result = new ArrayList<>();
    if (hobby == null) return result;
    for (String item : hobby.split(",")) {
      String trimmed = item.trim();
      if (!trimmed.isEmpty()) result.add(trimmed);
    }
    return result;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getTel() {
    return tel;
  }

  public void setTel(String tel) {
    this.tel = tel;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getAvatarUrl() {
    return avatar_url;
  }

  public void setAvatarUrl(String avatar_url) {
    this.avatar_url = avatar_url;
  }

  public String getHobby() {
    return hobby;
  }

  public void setHobby(String hobby) {
    this.hobby = hobby;
  }
}
