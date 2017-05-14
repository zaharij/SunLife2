package life.centaurs.sunlife.video.render.display;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import life.centaurs.sunlife.R;

import static life.centaurs.sunlife.video.render.constants.DisplayConstants.SCREENSHOT_FRAME_CHANGE_DURATION;
import static life.centaurs.sunlife.video.render.display.CameraNavigationFragment.isProcessing;

public class ScreenshotAnimator {
    private Context context;
    private LinearLayout mGallery;
    private LayoutInflater mInflater;
    private static Map<String, Integer> screenshotHashMap = new HashMap<>();
    public static boolean screenshotIsOutputed = false;
    private Fragment currentFragment;

    public static Map<String, Integer> getScreenshotHashMap() {
        return screenshotHashMap;
    }

    public ScreenshotAnimator(Fragment currentFragment, Context context, LinearLayout mGallery) {
        this.currentFragment = currentFragment;
        this.context = context;
        this.mGallery = mGallery;
        this.mInflater = LayoutInflater.from(context);
    }

    public void refreshScreenshotsAnim(Map<String, ArrayList<File>> chunksScreenshots){

    }

    public void startScreenshotAnim(ArrayList<File> imageFiles) {
        if (imageFiles != null) {
            int hashCodeOfImageFiles = imageFiles.hashCode();
            View view = mInflater.inflate(R.layout.activity_gallery_item, mGallery, false);
            ImageView imageView = (ImageView) view.findViewById(R.id.id_index_gallery_item_image);
            currentFragment.registerForContextMenu(view);
            int firstIndex = view.toString().indexOf("{") + 1;
            int lastIndex = view.toString().indexOf(" ");
            String keyChunkStr = view.toString().substring(firstIndex, lastIndex);
            screenshotHashMap.put(keyChunkStr, hashCodeOfImageFiles);
            new ChunkAnim(imageView, imageFiles, context);
            mGallery.addView(view);
            ChunksManager.screenshotNumberNameIsTaken = false;
        }
    }

    private class ChunkAnim{
        private ArrayList<File> imageFiles;
        private ImageView imageView;
        private Handler handler = new Handler();
        private int count = 0;
        private Context context;

        ChunkAnim(ImageView imageView, ArrayList<File> images, Context context){
            this.imageView = imageView;
            this.imageFiles = images;
            this.context = context;
            startAnim();
        }

        private void startAnim(){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    do {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                count = count < imageFiles.size() - 1 ? count + 1 : 0;
                                if (imageFiles.get(count).exists()) {
                                    Bitmap myBitmap = BitmapFactory.decodeFile(imageFiles.get(count).getAbsolutePath());
                                    imageView.setImageBitmap(myBitmap);
                                }
                            }
                        });
                        try {
                            Thread.sleep(SCREENSHOT_FRAME_CHANGE_DURATION);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } while (imageFiles.size() > 1 && !isProcessing());
                }
            }).start();
        }
    }
}
