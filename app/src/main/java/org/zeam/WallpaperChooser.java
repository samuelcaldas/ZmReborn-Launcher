package org.zeam;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import java.io.IOException;
import java.util.ArrayList;

public class WallpaperChooser extends Activity implements AdapterView.OnItemSelectedListener, View.OnClickListener {
    /* access modifiers changed from: private */
    public Bitmap mBitmap;
    private Gallery mGallery;
    /* access modifiers changed from: private */
    public ImageView mImageView;
    /* access modifiers changed from: private */
    public ArrayList<Integer> mImages;
    private boolean mIsWallpaperSet;
    /* access modifiers changed from: private */
    public WallpaperLoader mLoader;
    /* access modifiers changed from: private */
    public ArrayList<Integer> mThumbs;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtil.wrap(base));
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(1);
        findWallpapers();
        setContentView(R.layout.wallpaper_chooser);
        this.mGallery = (Gallery) findViewById(R.id.gallery);
        this.mGallery.setAdapter(new ImageAdapter(this));
        this.mGallery.setOnItemSelectedListener(this);
        this.mGallery.setCallbackDuringFling(false);
        findViewById(R.id.set).setOnClickListener(this);
        this.mImageView = (ImageView) findViewById(R.id.wallpaper);
    }

    private void findWallpapers() {
        this.mThumbs = new ArrayList<>(24);
        this.mImages = new ArrayList<>(24);
        Resources resources = getResources();
        String packageName = getApplication().getPackageName();
        addWallpapers(resources, packageName, R.array.wallpapers);
        addWallpapers(resources, packageName, R.array.extra_wallpapers);
    }

    private void addWallpapers(Resources resources, String packageName, int list) {
        int thumbRes;
        for (String extra : resources.getStringArray(list)) {
            int res = resources.getIdentifier(extra, "drawable", packageName);
            if (!(res == 0 || (thumbRes = resources.getIdentifier(String.valueOf(extra) + "_small", "drawable", packageName)) == 0)) {
                this.mThumbs.add(Integer.valueOf(thumbRes));
                this.mImages.add(Integer.valueOf(res));
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onResume() {
        super.onResume();
        this.mIsWallpaperSet = false;
    }

    /* access modifiers changed from: protected */
    public void onDestroy() {
        super.onDestroy();
        if (this.mLoader != null && this.mLoader.getStatus() != AsyncTask.Status.FINISHED) {
            this.mLoader.cancel(true);
            this.mLoader = null;
        }
    }

    public void onItemSelected(AdapterView parent, View v, int position, long id) {
        if (!(this.mLoader == null || this.mLoader.getStatus() == AsyncTask.Status.FINISHED)) {
            this.mLoader.cancel();
        }
        this.mLoader = (WallpaperLoader) new WallpaperLoader().execute(new Integer[]{Integer.valueOf(position)});
    }

    private void selectWallpaper(int position) {
        if (!this.mIsWallpaperSet) {
            this.mIsWallpaperSet = true;
            try {
                ((WallpaperManager) getSystemService("wallpaper")).setResource(this.mImages.get(position).intValue());
                setResult(-1);
                finish();
            } catch (IOException e) {
                Log.e(Launcher.LOG_TAG, "Failed to set wallpaper: " + e);
            }
        }
    }

    public void onNothingSelected(AdapterView parent) {
    }

    private class ImageAdapter extends BaseAdapter {
        private LayoutInflater mLayoutInflater;

        ImageAdapter(WallpaperChooser context) {
            this.mLayoutInflater = context.getLayoutInflater();
        }

        public int getCount() {
            return WallpaperChooser.this.mThumbs.size();
        }

        public Object getItem(int position) {
            return Integer.valueOf(position);
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView image;
            if (convertView == null) {
                image = (ImageView) this.mLayoutInflater.inflate(R.layout.wallpaper_item, parent, false);
            } else {
                image = (ImageView) convertView;
            }
            int thumbRes = ((Integer) WallpaperChooser.this.mThumbs.get(position)).intValue();
            image.setImageResource(thumbRes);
            Drawable thumbDrawable = image.getDrawable();
            if (thumbDrawable != null) {
                thumbDrawable.setDither(true);
            } else {
                Log.e(Launcher.LOG_TAG, String.format("Error decoding thumbnail resId=%d for wallpaper #%d", new Object[]{Integer.valueOf(thumbRes), Integer.valueOf(position)}));
            }
            return image;
        }
    }

    public void onClick(View v) {
        selectWallpaper(this.mGallery.getSelectedItemPosition());
    }

    class WallpaperLoader extends AsyncTask<Integer, Void, Bitmap> {
        BitmapFactory.Options mOptions = new BitmapFactory.Options();

        WallpaperLoader() {
            this.mOptions.inDither = false;
            this.mOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        }

        /* access modifiers changed from: protected */
        public Bitmap doInBackground(Integer... params) {
            if (isCancelled()) {
                return null;
            }
            try {
                return BitmapFactory.decodeResource(WallpaperChooser.this.getResources(), ((Integer) WallpaperChooser.this.mImages.get(params[0].intValue())).intValue(), this.mOptions);
            } catch (OutOfMemoryError e) {
                return null;
            }
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                if (isCancelled() || this.mOptions.mCancel) {
                    bitmap.recycle();
                    return;
                }
                if (WallpaperChooser.this.mBitmap != null) {
                    WallpaperChooser.this.mBitmap.recycle();
                }
                ImageView view = WallpaperChooser.this.mImageView;
                view.setImageBitmap(bitmap);
                WallpaperChooser.this.mBitmap = bitmap;
                Drawable drawable = view.getDrawable();
                drawable.setFilterBitmap(true);
                drawable.setDither(true);
                view.postInvalidate();
                WallpaperChooser.this.mLoader = null;
            }
        }

        /* access modifiers changed from: package-private */
        public void cancel() {
            this.mOptions.requestCancelDecode();
            super.cancel(true);
        }
    }
}
