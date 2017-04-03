package life.centaurs.sunlife.animation;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageSwitcher;

import static life.centaurs.sunlife.constants.AnimationConstants.*;

/**
 * AnimationSunLife
 * This class describes all animation of the program;
 * All methods are static, you don't need to create a new object
 */
public class AnimationSunLife {

    /**
     * sets alpha animation (emergence) to current ImageSwitcher
     * @param imageSwitcher
     */
    public static void imageEmergenceAlphaAnimation(ImageSwitcher imageSwitcher) {
        Animation inAnimation = new AlphaAnimation(0, 1);
        inAnimation.setDuration(IN_IMAGE_EMERGENCE_ALPHA_ANIMATION_DURATION);
        Animation outAnimation = new AlphaAnimation(1, 0);
        outAnimation.setDuration(OUT_IMAGE_EMERGENCE_ALPHA_ANIMATION_DURATION);

        imageSwitcher.setInAnimation(inAnimation);
        imageSwitcher.setOutAnimation(outAnimation);
    }

}
