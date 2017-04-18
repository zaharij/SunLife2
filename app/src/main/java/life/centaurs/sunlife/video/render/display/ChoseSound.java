package life.centaurs.sunlife.video.render.display;


import android.content.Context;
import android.content.res.AssetManager;
import android.os.Handler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import static life.centaurs.sunlife.video.render.constants.DisplayConstants.ASSETS_SOUNDS_ARRAY;
import static life.centaurs.sunlife.video.render.constants.DisplayConstants.getMediaDir;

public class ChoseSound {
    private final static String ASSETS_SOUNDS_FOLDER_NAME = "sounds";
    private static String sound = null;
    private Context context;
    private Handler handler = new Handler();

    public ChoseSound(Context context) {
        this.context = context;
    }

    public static String getSound() {
        return sound;
    }

    public void chooseAndCopyAssetsSoundToSd(){
        Random random = new Random();
        int soundNumber = random.nextInt(ASSETS_SOUNDS_ARRAY.length);
        sound = ASSETS_SOUNDS_ARRAY[soundNumber];
        new Thread(new Runnable() {
            @Override
            public void run() {
                copyAssets(ASSETS_SOUNDS_FOLDER_NAME, new String[]{sound}, getMediaDir().getAbsolutePath() + "/");
                handler.post(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
        }).start();
    }


    private void copyAssets(String folderNameIn, String[] filesToCopy, String pathNameOut){
        AssetManager assetManager = context.getAssets();
        String[] files = null;
        try {
            files = assetManager.list(folderNameIn);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(String filename: files){
            for (int i = 0; i < filesToCopy.length; i++) {
                if (filename.equals(filesToCopy[i])) {
                    InputStream in = null;
                    OutputStream out = null;
                    try {
                        in = assetManager.open(folderNameIn.concat("/").concat(filename));
                        out = new FileOutputStream(pathNameOut.concat(filename));
                        copyFiles(in, out);
                        in.close();
                        in = null;
                        out.flush();
                        out.close();
                        out = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void copyFiles(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read=in.read(buffer))!=-1){
            out.write(buffer, 0, read);
        }
    }
}
