package com.shuiyes.mocklocation.util;

import com.shuiyes.mocklocation.R;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.Toast;

public class ToastUtil{

    /**
     * Toast align windows top
     * @param context
     * @return
     */
    public static Toast createToast(Context context) {
        Toast toast = new Toast(context);
        toast.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.setView(LayoutInflater.from(context).inflate(R.layout.transient_notification, null));
        return toast;
    }

    public static void show(Context context, CharSequence text){
        show(context, text, 0);
    }

    private static Toast mToast;
    public static void show(Context context, CharSequence text, int duration){
        if(mToast == null) mToast = createToast(context);
        mToast.setDuration(duration);
        mToast.setText(text);
        mToast.show();
    }

    public static void show(Context context, int resid, int duration){
        if(mToast == null) mToast = createToast(context);
        mToast.setDuration(duration);
        mToast.setText(resid);
        mToast.show();
    }

}
