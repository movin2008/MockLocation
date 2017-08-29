package com.shuiyes.mocklocation;

import com.baidu.mapapi.model.LatLng;
import com.shuiyes.mocklocation.util.Constants;
import com.shuiyes.mocklocation.util.NumberUtils;
import com.shuiyes.mocklocation.util.ToastUtil;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.TextView;

public class FullScreenDialog extends Dialog implements View.OnClickListener {

    private Context mContext = null;

    public FullScreenDialog(Context context, LatLng latLng, String adress) {
        super(context, R.style.FullScreenDialog);
        mContext = context;
        mLatLng = latLng;
        mAdress = adress;
    }

    private LatLng mLatLng;
    private String mAdress;
    private TextView mText;
    private Button mCancel;
    private Button mConfim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.full_screen_dialog);

        mText = (TextView) this.findViewById(R.id.text);
        String text = "<font color='#4876ff'>gps："+NumberUtils.numFormat(mLatLng.latitude)+"/"+NumberUtils.numFormat(mLatLng.longitude)+"</font><br>";
        text += "<font color='#4078c0'>"+mAdress+"</font>";
        mText.setText(Html.fromHtml(text));

        mConfim = (Button) this.findViewById(R.id.confim);
        mCancel = (Button) this.findViewById(R.id.cancel);

        //mConfim.setOnClickListener(this);
        mCancel.setOnClickListener(this);
    }

    public void setConfimClickListener(View.OnClickListener l){
        mConfim.setOnClickListener(l);
    }

    @Override
    public void show() {
        super.show();

        final LayoutParams layoutParams = getWindow().getAttributes();
        // align bottom
        layoutParams.gravity=Gravity.BOTTOM;
        // fullscreen
        layoutParams.width= LayoutParams.MATCH_PARENT;
        layoutParams.height= LayoutParams.WRAP_CONTENT;

        getWindow().getDecorView().setPadding(0, 0, 0, 0);
        getWindow().setAttributes(layoutParams);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
        case R.id.confim:
            break;
        }
        dismiss();
    }
}
