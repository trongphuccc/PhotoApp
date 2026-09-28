package thanhdnh.ueh.edu.article_app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class UserAdapter extends BaseAdapter {
  private ArrayList<UserProfile> user_list;
  private Context context;

  public UserAdapter(ArrayList<UserProfile> user_list, Context context) {
    this.user_list = user_list;
    this.context = context;
  }

  @Override
  public int getCount() {
    return user_list.size();
  }

  @Override
  public Object getItem(int position) {
    return user_list.get(position);
  }

  @Override
  public long getItemId(int position) {
    return user_list.get(position).getId();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    final MyView dataitem;
    if (convertView == null) {
      dataitem = new MyView();
      convertView = LayoutInflater.from(context).inflate(R.layout.user_disp_tpl, parent, false);
      dataitem.iv_photo = convertView.findViewById(R.id.imv_photo);
      dataitem.tv_caption = convertView.findViewById(R.id.tv_username);
      convertView.setTag(dataitem);
    } else {
      dataitem = (MyView) convertView.getTag();
    }

    UserProfile user = user_list.get(position);
    String avatarUrl = user.getAvatarUrl();
    // Picasso báo lỗi nếu URL là chuỗi rỗng; còn null thì chỉ hiện placeholder
    if (avatarUrl != null && avatarUrl.trim().isEmpty()) avatarUrl = null;

    // Ô lưới: ảnh = avatar (cắt giữa cho vừa ô vuông), chữ = username
    Picasso.get()
        .load(avatarUrl)
        .placeholder(R.drawable.avatar_placeholder)
        .error(R.drawable.avatar_placeholder)
        .resize(300, 300)
        .centerCrop()
        .into(dataitem.iv_photo);
    dataitem.tv_caption.setText(user.getUsername());
    return convertView;
  }

  private static class MyView {
    ImageView iv_photo;
    TextView tv_caption;
  }
}
