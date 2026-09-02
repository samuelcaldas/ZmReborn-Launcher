package org.zmreborn;

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

/**
 * Activity for choosing and setting built-in launcher wallpapers.
 */
public class WallpaperChooser extends Activity implements AdapterView.OnItemSelectedListener, View.OnClickListener {
    private Bitmap bitmap;
    private Gallery gallery;
    private ImageView imageView;
    private ArrayList<Integer> images;
    private boolean isWallpaperSet;
    private WallpaperLoader loader;
    private ArrayList<Integer> thumbs;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtil.wrap(base));
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(1);
        findWallpapers();
        setContentView(R.layout.wallpaper_chooser);
        this.imageView = (ImageView) findViewById(R.id.wallpaper);
        this.gallery = (Gallery) findViewById(R.id.gallery);
        this.gallery.setAdapter(new ImageAdapter(this));
        this.gallery.setOnItemSelectedListener(this);
        this.gallery.setCallbackDuringFling(false);
        View setButton = findViewById(R.id.set);
        setButton.setEnabled(!this.images.isEmpty());
        setButton.setOnClickListener(this);
    }

    private void findWallpapers() {
        this.thumbs = new ArrayList<>(24);
        this.images = new ArrayList<>(24);
        Resources resources = getResources();
        String packageName = getApplication().getPackageName();
        addWallpapers(resources, packageName, R.array.wallpapers);
        addWallpapers(resources, packageName, R.array.extra_wallpapers);
    }

    private void addWallpapers(Resources resources, String packageName, int list) {
        for (String extra : resources.getStringArray(list)) {
            int res = resources.getIdentifier(extra, "drawable", packageName);
            if (res == 0) {
                continue;
            }
            int thumbRes = resources.getIdentifier(extra + "_small", "drawable", packageName);
            if (thumbRes == 0) {
                continue;
            }
            this.thumbs.add(Integer.valueOf(thumbRes));
            this.images.add(Integer.valueOf(res));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.isWallpaperSet = false;
        View setButton = findViewById(R.id.set);
        if (setButton != null) {
            setButton.setEnabled(this.images != null && !this.images.isEmpty());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.loader != null && this.loader.getStatus() != AsyncTask.Status.FINISHED) {
            this.loader.cancel(true);
            this.loader = null;
        }
        if (this.bitmap != null) {
            this.bitmap.recycle();
            this.bitmap = null;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
        if (!isWallpaperPosition(position)) {
            return;
        }
        if (this.loader != null && this.loader.getStatus() != AsyncTask.Status.FINISHED) {
            this.loader.cancel();
        }
        this.loader = (WallpaperLoader) new WallpaperLoader().execute(Integer.valueOf(position));
    }

    private void selectWallpaper(int position) {
        if (this.isWallpaperSet || !isWallpaperPosition(position)) {
            return;
        }
        WallpaperManager wallpaperManager = (WallpaperManager) getSystemService("wallpaper");
        if (wallpaperManager == null) {
            Log.e(Launcher.LOG_TAG, "Wallpaper service unavailable");
            return;
        }
        try {
            wallpaperManager.setResource(this.images.get(position).intValue());
            this.isWallpaperSet = true;
            setResult(-1);
            finish();
        } catch (IOException e) {
            Log.e(Launcher.LOG_TAG, "Failed to set wallpaper: " + e);
        } catch (SecurityException e) {
            Log.e(Launcher.LOG_TAG, "Wallpaper permission denied", e);
        }
    }

    private boolean isWallpaperPosition(int position) {
        return this.images != null && position >= 0 && position < this.images.size();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private class ImageAdapter extends BaseAdapter {
        private final LayoutInflater layoutInflater;

        ImageAdapter(WallpaperChooser context) {
            this.layoutInflater = context.getLayoutInflater();
        }

        @Override
        public int getCount() {
            return WallpaperChooser.this.thumbs.size();
        }

        @Override
        public Object getItem(int position) {
            return Integer.valueOf(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView == null
                    ? this.layoutInflater.inflate(R.layout.wallpaper_item, parent, false)
                    : convertView;
            ImageView image = (ImageView) view;
            int thumbRes = WallpaperChooser.this.thumbs.get(position).intValue();
            image.setImageResource(thumbRes);
            Drawable thumbDrawable = image.getDrawable();
            if (thumbDrawable != null) {
                thumbDrawable.setDither(true);
            } else {
                Log.e(Launcher.LOG_TAG, String.format(
                        "Error decoding thumbnail resId=%d for wallpaper #%d",
                        Integer.valueOf(thumbRes), Integer.valueOf(position)));
            }
            return image;
        }
    }

    @Override
    public void onClick(View v) {
        selectWallpaper(this.gallery.getSelectedItemPosition());
    }

    class WallpaperLoader extends AsyncTask<Integer, Void, Bitmap> {
        BitmapFactory.Options options = new BitmapFactory.Options();

        WallpaperLoader() {
            this.options.inDither = false;
            this.options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        }

        @Override
        protected Bitmap doInBackground(Integer... params) {
            if (isCancelled() || params == null || params.length == 0
                    || !WallpaperChooser.this.isWallpaperPosition(params[0].intValue())) {
                return null;
            }
            try {
                return BitmapFactory.decodeResource(WallpaperChooser.this.getResources(),
                        WallpaperChooser.this.images.get(params[0].intValue()).intValue(), this.options);
            } catch (OutOfMemoryError e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap loadedBitmap) {
            if (loadedBitmap == null) {
                return;
            }
            if (isCancelled() || this.options.mCancel) {
                loadedBitmap.recycle();
                return;
            }
            if (WallpaperChooser.this.bitmap != null) {
                WallpaperChooser.this.bitmap.recycle();
            }
            ImageView view = WallpaperChooser.this.imageView;
            if (view == null) {
                loadedBitmap.recycle();
                return;
            }
            view.setImageBitmap(loadedBitmap);
            WallpaperChooser.this.bitmap = loadedBitmap;
            Drawable drawable = view.getDrawable();
            if (drawable != null) {
                drawable.setFilterBitmap(true);
                drawable.setDither(true);
            }
            view.postInvalidate();
            WallpaperChooser.this.loader = null;
        }

        void cancel() {
            this.options.requestCancelDecode();
            super.cancel(true);
        }
    }
}
