package thanhdnh.ueh.edu.article_app;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import com.squareup.picasso.Transformation;

// Phép biến đổi ảnh cho Picasso: bo tròn 4 góc của bitmap.
// Picasso gọi transform() trên luồng nền, SAU các bước resize/centerCrop/rotate.
public class RoundedCornersTransformation implements Transformation {
  private final int radius; // bán kính bo góc, đơn vị pixel

  public RoundedCornersTransformation(int radius) {
    this.radius = radius;
  }

  @Override
  public Bitmap transform(Bitmap source) {
    // Tạo bitmap mới cùng kích thước, nền trong suốt
    Bitmap output = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(output);

    // Dùng ảnh gốc làm "màu tô" (shader), rồi vẽ một hình chữ nhật bo góc
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
    RectF rect = new RectF(0, 0, source.getWidth(), source.getHeight());
    canvas.drawRoundRect(rect, radius, radius, paint);

    // Quy tắc của Picasso: trả về bitmap mới thì phải giải phóng bitmap cũ
    source.recycle();
    return output;
  }

  // Khóa để Picasso phân biệt ảnh trong bộ nhớ đệm (mỗi bán kính là một khóa khác)
  @Override
  public String key() {
    return "rounded_corners(radius=" + radius + ")";
  }
}
