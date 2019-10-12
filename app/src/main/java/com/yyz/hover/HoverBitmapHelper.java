/**
 *
 */
package com.yyz.hover;


import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import storage.FileHelper;

/**
 * @className:BitmapUtils.java
 * @classDescription:图片工具
 * @author: DANIEL DENG
 * @createTime: 2013-3-5
 */
public class HoverBitmapHelper {

    public static final int MAX_BITMAP_SIZE = 5096;

    public static boolean isBigBitmap(byte[] bitmapData) {
        if (bitmapData == null) {
            return false;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length, options);
        int width = options.outWidth;
        int height = options.outHeight;
        return width > MAX_BITMAP_SIZE || height > MAX_BITMAP_SIZE;
    }

    public static boolean isBigBitmap(String path) {
        if (path == null) {
            return false;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        int width = options.outWidth;
        int height = options.outHeight;
        return width > HoverBitmapHelper.MAX_BITMAP_SIZE || height > HoverBitmapHelper.MAX_BITMAP_SIZE;
    }

    /**
     * 读取照片exif信息中的旋转角度
     * @param path 照片路径
     * @return角度
     */
    public static int getPhotoDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    /**
     * 旋转图片
     * @param img
     * @param degree
     * @return
     */
    public static Bitmap rotateBitmap(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        int width = img.getWidth();
        int height = img.getHeight();
        img = Bitmap.createBitmap(img, 0, 0, width, height, matrix, true);
        return img;
    }

    /**
     * 根据计算的inSampleSize，得到压缩后图片
     *
     * @param data      图片数据
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static Bitmap decodeBitmap(byte[] data, int reqWidth, int reqHeight) {
        if (data == null || reqWidth <= 0 || reqHeight <= 0) {
            return null;
        }
        // 第一次解析将inJustDecodeBounds设置为true，来获取图片大小
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);
        int width = options.outWidth;
        int height = options.outHeight;
        if (width > HoverBitmapHelper.MAX_BITMAP_SIZE || height > HoverBitmapHelper.MAX_BITMAP_SIZE) {
            //设置显示图片的中心区域
            Bitmap bitmap = null;
            try {
                BitmapRegionDecoder bitmapRegionDecoder = BitmapRegionDecoder.newInstance(data, 0, data.length, false);
                BitmapFactory.Options decoderOptions = new BitmapFactory.Options();
                decoderOptions.inPreferredConfig = Bitmap.Config.RGB_565;
                int right = width > HoverBitmapHelper.MAX_BITMAP_SIZE ? HoverBitmapHelper.MAX_BITMAP_SIZE / 2 : width;
                int bottom = height > HoverBitmapHelper.MAX_BITMAP_SIZE ? HoverBitmapHelper.MAX_BITMAP_SIZE / 2 : height;
                bitmap = bitmapRegionDecoder.decodeRegion(new Rect(0, 0, right, bottom), decoderOptions);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                return bitmap;
            }
        }

        // 调用上面定义的方法计算inSampleSize值
        calculateInSampleSize(options, reqWidth, reqHeight);
        // 使用获取到的inSampleSize值再次解析图片
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeByteArray(data, 0, data.length, options);

    }

    public static Bitmap decodeBitmap(String pathName, int reqWidth, int reqHeight) {
        // 第一次解析将inJustDecodeBounds设置为true，来获取图片大小
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, options);

        int width = options.outWidth;
        int height = options.outHeight;
        if (width > HoverBitmapHelper.MAX_BITMAP_SIZE || height > HoverBitmapHelper.MAX_BITMAP_SIZE) {
            //设置显示图片的中心区域
            Bitmap bitmap = null;
            try {
                BitmapRegionDecoder bitmapRegionDecoder = BitmapRegionDecoder.newInstance(pathName, false);
                BitmapFactory.Options decoderOptions = new BitmapFactory.Options();
                decoderOptions.inPreferredConfig = Bitmap.Config.RGB_565;
                int right = width > HoverBitmapHelper.MAX_BITMAP_SIZE ? HoverBitmapHelper.MAX_BITMAP_SIZE / 2 : width;
                int bottom = height > HoverBitmapHelper.MAX_BITMAP_SIZE ? HoverBitmapHelper.MAX_BITMAP_SIZE / 2 : height;
                bitmap = bitmapRegionDecoder.decodeRegion(new Rect(0, 0, right, bottom), decoderOptions);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                return bitmap;
            }
        }
        // 调用上面定义的方法计算inSampleSize值
        calculateInSampleSize(options, reqWidth, reqHeight);
        // 使用获取到的inSampleSize值再次解析图片
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(pathName, options);
    }

    /**
     * 计算inSampleSize，用于压缩图片
     *
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static void calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // 源图片的宽度
        int inSampleSize = 1;
        if (options.outWidth > reqWidth && options.outHeight > reqHeight) {
            // 计算出实际宽度和目标宽度的比率
            int widthRatio = Math.round((float) options.outWidth / (float) reqWidth);
            int heightRatio = Math.round((float) options.outHeight / (float) reqWidth);
            inSampleSize = Math.max(widthRatio, heightRatio);
        }
        options.inSampleSize = inSampleSize;
    }

    public static void compress(String srcPath, String decPath, int quality) {
        if (TextUtils.isEmpty(srcPath) || TextUtils.isEmpty(decPath)) {
            return;
        }
        quality = quality > 0 ? quality : 80;
        FileOutputStream fos = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] photoData;
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(srcPath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            photoData = baos.toByteArray();
            File file = new File(decPath);
            boolean exists = file.getParentFile().exists();
            if (!exists) {
                exists = file.getParentFile().mkdirs();
            }
            if (exists) {
                fos = new FileOutputStream(file);
                fos.write(photoData);
            }
        } catch (Throwable e) {
//            System.gc();
            e.printStackTrace();
        } finally {
            try {
                baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 转换成圆角的bitmap
     *
     * @param sourceBitmap
     * @param roundPx
     * @return
     */
    public static Bitmap getRoundedCornerBitmap(Bitmap sourceBitmap, float roundPx) {
        if (sourceBitmap == null && roundPx < 0) {
            return null;
        }

        int width = sourceBitmap.getWidth();
        int height = sourceBitmap.getHeight();
        //画板
        Bitmap bitmap = Bitmap.createBitmap(width, height, sourceBitmap.getConfig());
        Paint paint = new Paint();
        Canvas canvas = new Canvas(bitmap);
        paint.setAntiAlias(true);
        paint.setShader(new BitmapShader(sourceBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        //画圆角背景
        RectF rectF = new RectF(new Rect(0, 0, width, height));
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        sourceBitmap.recycle();
        return bitmap;

//        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(output);
//
////        final int color = 0xff424242;
//        final Paint paint = new Paint();
//        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
//        final RectF rectF = new RectF(rect);
//
//        paint.setAntiAlias(true);
////        canvas.drawARGB(0, 0, 0, 0);
////        paint.setColor(color);
//        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
//
//        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
//        canvas.drawBitmap(bitmap, rect, rect, paint);
//
//        return output;
    }

    /**
     * 转换成圆形的bitmap
     *
     * @param bitmap
     * @return
     */
    public static Bitmap getOvalBitmap(Bitmap bitmap) {

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

//        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
//        paint.setColor(color);

        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    public static byte[] bitmapToByte(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return null;
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] data = stream.toByteArray();
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public static Bitmap byteToBitmap(byte[] bitmapData) {
        Bitmap bitmap = null;
        if (bitmapData != null && bitmapData.length > 0) {
            bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
        }
        return bitmap;
    }

    public static Bitmap readImageBitmap(ImageView imageView) {
        if (imageView == null) {
            return null;
        }
        Drawable drawable = imageView.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            return bitmapDrawable.getBitmap();
        }
        return null;
    }

    public static byte[] readImageBitmapByte(ImageView imageView) {
        byte[] data = null;
        if (imageView == null) {
            return data;
        }
        Drawable drawable = imageView.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            if (bitmap == null || bitmap.isRecycled()) {
                return data;
            }
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            boolean ret = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            if (ret) {
                data = stream.toByteArray();
            }
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return data;
    }

    /**
     * 读取压缩的资源图片
     *
     * @param resId        资源图片ID
     * @param inSampleSize 压缩图片到原来的几分之一
     * @return BitmapDrawable对象
     */
    public static BitmapDrawable readCompressedBitmapDrawable(Resources resources, int resId, int inSampleSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inSampleSize = inSampleSize;

        Bitmap bm = BitmapFactory.decodeStream(resources.openRawResource(resId), null, options);
        return new BitmapDrawable(resources, bm);
    }

    /**
     * 读取最省内存的资源图片
     *
     * @param resId        资源图片ID
     * @param inSampleSize 压缩图片到原来的几分之一
     * @return BitmapDrawable对象
     */
    public static BitmapDrawable readOptimizedBitmapDrawable(Resources resources, int resId, int inSampleSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inJustDecodeBounds = false;
        options.inSampleSize = inSampleSize;

        Bitmap bm = BitmapFactory.decodeStream(resources.openRawResource(resId), null, options);
        return new BitmapDrawable(resources, bm);
    }

    /**
     * Drawable → Bitmap
     *
     * @param drawable
     * @return
     */
    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                        : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        //canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * 回收背景图片资源
     *
     * @param viewGroup 布局等组件
     */
    public static void recycleBitmapDrawable(ViewGroup viewGroup) {
        try {
            //回收启动页背景图片
            viewGroup.setBackgroundResource(0);
            viewGroup.setBackgroundColor(Color.WHITE);
            Object object = viewGroup.getBackground();
            if (object instanceof BitmapDrawable) {
                BitmapDrawable drawable = (BitmapDrawable) object;
                drawable.setCallback(null);
                Bitmap bitmap = drawable.getBitmap();
                if (!bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void releaseImageView(ImageView imageView) {
        if (imageView == null) {
            return;
        }
        Drawable drawable = imageView.getDrawable();
        if (drawable != null && drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        drawable = imageView.getBackground();
        if (drawable != null && drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    public static Drawable loadImageFromNetwork(String imageUrl) {
        Drawable drawable = null;
        try {
            // 可以在这里通过文件名来判断，是否本地有此图片
            drawable = Drawable.createFromStream(
                    new URL(imageUrl).openStream(), "bitmap.jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return drawable;
    }


    /**
     * 获取Assets中的图片
     *
     * @param
     * @return
     * @author swallow
     * @createTime 2015/9/7
     * @lastModify 2015/9/7
     */
    public static Bitmap getImageFromAssetsFile(Context context, String fileName) {
        Bitmap image = null;
        AssetManager am = context.getAssets();
        try {
            InputStream is = am.open(fileName);
            image = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return image;

    }

    /**
     * 将view转换成bitmap对象
     *
     * @param view
     * @return
     */
    public static Bitmap createBitmapByView(View view) {
        if (view == null) {
            return null;
        }
        int width = view.getWidth();
        int height = view.getHeight();
        if (width == 0 || height == 0) {
            return null;
        }
        try {
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            view.draw(canvas);
            return bitmap;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap readImage(String absPath) {
        if (TextUtils.isEmpty(absPath)) {
            return null;
        }

        if (!FileHelper.isExist(absPath)) {
            return null;
        }

        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeFile(absPath);
        } catch (Throwable e) {

        }

        return bitmap;
    }

    public static boolean saveImage(ImageView imageView, String absPath, String fileName) {
        if (imageView == null || TextUtils.isEmpty(absPath) || TextUtils.isEmpty(fileName)) {
            return false;
        }
        Drawable drawable = imageView.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                return saveImage(bitmap, absPath, fileName, 100);
            }
        }
        return false;
    }

    public static boolean saveImage(Bitmap bitmap, String absPath, String fileName) {
        return saveImage(bitmap, absPath, fileName, 100);
    }

    public static boolean saveImage(Bitmap bitmap, String absPath, String fileName, int quality) {
        boolean ret = false;
        if (bitmap == null || bitmap.isRecycled() || TextUtils.isEmpty(absPath) || TextUtils.isEmpty(fileName)) {
            return ret;
        }
        FileOutputStream fos = null;
        try {
            File dir = new File(absPath);
            boolean exists = dir.exists();
            if (!exists) {
                exists = dir.mkdirs();
            }
            if (exists) {
                File outFile = new File(dir, fileName);
                fos = new FileOutputStream(outFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos);
                ret = true;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return ret;
    }

}
