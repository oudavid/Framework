package com.aim.framework;

import android.content.Context;
import android.os.Vibrator;

/**
 * Created by Administrator on 2/18/15.
 */
public class HapticFeedbackUtils {
    private static Vibrator sVibrator;

    public static void provideHapticFeedback(Context context) {
        if (sVibrator == null)
            sVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        sVibrator.vibrate(50);
    }
}
