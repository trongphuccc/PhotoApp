package thanhdnh.ueh.edu.article_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
  // File JSON đặt trong repo GitHub (phải push data/users.json lên nhánh master thì URL mới có dữ liệu)
  public static final String USERS_URL = "https://raw.githubusercontent.com/trongphuccc/PhotoApp/master/data/users.json";

  public GridView gridview;

  private AdapterView.OnItemClickListener onitemclick = new AdapterView.OnItemClickListener() {
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      // Gửi id của user được chọn sang màn hình Detail qua Intent extra
      UserProfile user = (UserProfile) parent.getItemAtPosition(position);
      Intent intent = new Intent(MainActivity.this, ViewUserActivity.class);
      intent.putExtra(ViewUserActivity.EXTRA_USER_ID, user.getId());
      startActivity(intent);
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    getSupportActionBar().hide();

    gridview = findViewById(R.id.gridview);
    ProgressBar pb_loading = findViewById(R.id.pb_loading);
    TextView tv_loading = findViewById(R.id.tv_loading);

    new UserData(this, gridview, pb_loading, tv_loading).loadData(USERS_URL);
    gridview.setOnItemClickListener(onitemclick);
  }

}
