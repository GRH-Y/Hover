package com.yyz.hover.joggle;

import android.graphics.Bitmap;
import android.widget.ImageView;

public interface IHoverLoadHandleListener {

    Bitmap onHandle(ImageView imageView, Bitmap sourceBitmap);
}
