package com.shuiyes.mocklocation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMap.OnMapClickListener;
import com.baidu.mapapi.map.BaiduMap.OnMapLongClickListener;
import com.baidu.mapapi.map.BaiduMap.OnMapRenderCallback;
import com.baidu.mapapi.map.BaiduMap.OnMapTouchListener;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.UiSettings;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.shuiyes.mocklocation.util.Constants;
import com.shuiyes.mocklocation.util.Haversine;
import com.shuiyes.mocklocation.util.NumberUtils;
import com.shuiyes.mocklocation.util.ToastUtil;

@SuppressLint("NewApi")
public class MainActivity extends Activity implements OnMapClickListener, OnGetGeoCoderResultListener, OnMapRenderCallback, OnMapLongClickListener {

    private static final String TAG = "SHUIYES";
    /** 反地理编码查询对象(经纬度->地址信息)  */ 
    private GeoCoder mGeoCoder = null;
    /** 反地理编码位置坐标 */
    private ReverseGeoCodeOption mReverseGeoCodeOption = null;
    /** 标记 */
    private MarkerOptions mMarkerOptions = null;
    private MapView mMapView = null;
    private BaiduMap mBaiduMap;
    /** 定位显示 */
    private TextView mLocationTextView = null;
    /** 穿越 */
    private Button mMockLocationButton = null;

    private Context mContext;
    private double mCompanyLat;
    private double mCompanyLng;

    @Override  
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        SDKInitializer.initialize(getApplicationContext());

        setContentView(R.layout.activity_main);

        mContext = this;
        mLocationTextView = (TextView) findViewById(R.id.location);
        mMockLocationButton = (Button) this.findViewById(R.id.mock_location);
        // 获取地图控件引用  
        mMapView = (MapView) findViewById(R.id.bmapView);

        //设置是否显示比例尺控件  
        mMapView.showScaleControl(true);
        //设置是否显示缩放控件  
        mMapView.showZoomControls(false);
        // 删除百度地图LoGo  
        mMapView.removeViewAt(1); 

        mBaiduMap = mMapView.getMap();
        mBaiduMap.setOnMapClickListener(this);
        mBaiduMap.setOnMapLongClickListener(this);
        mBaiduMap.setOnMapRenderCallbadk(this);

        UiSettings mUiSettings = mBaiduMap.getUiSettings();
        mUiSettings.setRotateGesturesEnabled(false);
        mUiSettings.setOverlookingGesturesEnabled(false);

        mGeoCoder = GeoCoder.newInstance();
        mGeoCoder.setOnGetGeoCodeResultListener(this);

        mReverseGeoCodeOption = new ReverseGeoCodeOption();
        // 设置marker图标  
        mMarkerOptions = new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.marker));

        final SharedPreferences sharedPreferences= getSharedPreferences("adress",Activity.MODE_PRIVATE);
        String lat = sharedPreferences.getString(Constants.LAT, null);
        String lng = sharedPreferences.getString(Constants.LNG, null);

        mCompanyLat = (lat != null)?Double.parseDouble(lat):Constants.ArchermindLat;
        mCompanyLng = (lng != null)?Double.parseDouble(lng):Constants.ArchermindLng;
        updateBaiduMapStatus(mCompanyLat,mCompanyLng,20);

        init();
    }

    /**
     * 更新地图中心点
     * @param lat 纬度
     * @param lng 经度
     * @param zoom 缩放值
     */
    private void updateBaiduMapStatus(double lat, double lng, int zoom){
        mLoadingDialog = showMapLoading();

        //设定中心点坐标 
        LatLng archermind = new LatLng(lat, lng);
        MapStatus mMapStatus = new MapStatus.Builder().target(archermind).zoom(zoom).build();
        // 改变地图状态
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(mMapStatus));
    }

    /**
     * 设置定位结果框，穿越按钮，搜索按钮的显示与否
     * 点击搜索后，要隐藏
     * @param state
     */
    private void setVisibility(int state){
        mLocationTextView.setVisibility(state);
        mMockLocationButton.setVisibility(state);
        this.findViewById(R.id.ib_search).setVisibility(state);
    }

    @Override  
    public boolean onMapPoiClick(MapPoi mp) {
        Log.d(TAG, "onMapPoiClick: " + mp.getPosition());
        marked(mp.getPosition());
        return false;
    }
    
    @Override  
    public void onMapClick(LatLng latLng) {
        Log.d(TAG, latLng.toString());
        marked(latLng);
    }

    private boolean mMapLongClick = false;
    @Override
    public void onMapLongClick(LatLng latLng) {
        mMapLongClick = true;
        reverseGeoCode(latLng);
    }


    /** 穿越的经纬度 */
    private LatLng mMockLatLng = null;
    private void marked(LatLng latLng){

        if(mIsMock){
            ToastUtil.show(mContext, "请先停止上次穿越数据");
            return;
        }

        mMockLocationButton.setEnabled(mHasAddTestProvider);
        mMockLatLng = Haversine.convertBaiduToGPS(latLng);

        mLocationTextView.setText("");
        mLocationTextView.setText("gps: "+NumberUtils.numFormat(mMockLatLng.latitude)+"/"+NumberUtils.numFormat(mMockLatLng.longitude)+"\n");

        double distence = Haversine.Distance(latLng.latitude, latLng.longitude, mCompanyLat, mCompanyLng);
        ToastUtil.show(mContext, "距离公司 "+distence+" 米");

        //先清除图层  
        mBaiduMap.clear();
        // 构建MarkerOption，用于在地图上添加Marker  
        mMarkerOptions.position(latLng);
        // 在地图上添加Marker，并显示  
        mBaiduMap.addOverlay(mMarkerOptions);

        reverseGeoCode(latLng);
    }
    
    private void reverseGeoCode(LatLng latLng){
        // 查找位置
        mReverseGeoCodeOption.location(latLng);
        mGeoCoder.reverseGeoCode(mReverseGeoCodeOption);
    }

    @Override  
    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult r) {
        if(mMapLongClick){
            mMapLongClick = false;
            final LatLng latlng = r.getLocation();
            final FullScreenDialog dialog = new FullScreenDialog(this, latlng, r.getAddress());
            dialog.show();
            dialog.setConfimClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCompanyLat = latlng.latitude;
                    mCompanyLng = latlng.longitude;

                    final SharedPreferences sharedPreferences= mContext.getSharedPreferences("adress",Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(Constants.LAT, mCompanyLat+"");
                    editor.putString(Constants.LNG, mCompanyLng+"");
                    editor.apply();

                    ToastUtil.show(mContext, "公司地址设置成功");
                    dialog.dismiss();
                }
            });
        }else{
            // 更新坐标地址
            mLocationTextView.setText(mLocationTextView.getText()+r.getAddress());
        }
    }

    @Override  
    public void onGetGeoCodeResult(GeoCodeResult r) {}

    /** 是否穿越  */
    private boolean mIsMock = false;

    /** 穿越按钮事件 */
    public void mockLocation(View v){
        if(mIsMock){
            // 停止穿越
            mIsMock = false;
            ((Button)v).setText(R.string.mock);
            v.setBackgroundResource(R.drawable.circle);
        }else{
            mIsMock = true;
            ((Button)v).setText(R.string.stop);
            v.setBackgroundResource(R.drawable.circle_red);

            ToastUtil.show(mContext, "穿越成功");

            // 启动穿越线程
            mHandler.post(mRunnableMockLocation);
        }
    }

    private static final int REQUEST_CODE_SEARCH = 101;
    /** 搜索按钮事件 */
    public void searchLocation(View v) {
        setVisibility(View.GONE);
        startActivityForResult(new Intent(this,SearchActivity.class), REQUEST_CODE_SEARCH);
        overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
    }

    private LocationManager mLocationManager;
    
    private final Handler mHandler = new Handler();
    /** 穿越线程(需要不停的设置虚拟坐标) */
    private final RunnableMockLocation mRunnableMockLocation = new RunnableMockLocation();
    private class RunnableMockLocation implements Runnable {
        @Override
        public void run() {
            // 模拟位置（addTestProvider成功的前提下）
            for (String providerStr : mMockProviders) {
                Location mockLocation = new Location(providerStr);
                float r = NumberUtils.random100();
                mockLocation.setLatitude(Double.parseDouble(NumberUtils.numFormat(mMockLatLng.latitude+(r/10000))));     // 维度（度）
                r = NumberUtils.random100();
                mockLocation.setLongitude(Double.parseDouble(NumberUtils.numFormat(mMockLatLng.longitude+(r/10000))));   // 经度（度）
                mockLocation.setAltitude(NumberUtils.random()*10);                                             // 高程（米）
                mockLocation.setBearing(NumberUtils.random()*10+140);                                                                  // 方向（度）
                mockLocation.setSpeed(NumberUtils.random());                                                                      // 速度（米/秒）
                mockLocation.setAccuracy((mSysytemAccuracy != 0)?mSysytemAccuracy:NumberUtils.random()*10);    // 精度（米）
                mockLocation.setTime(new Date().getTime());                                                    // 本地时间
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                }
                
                LocationProvider provider = mLocationManager.getProvider(providerStr);
                if (provider != null) {
                	//mLocationManager.removeTestProvider(providerStr);
//                	mLocationManager.addTestProvider(provider.getName(), provider.requiresNetwork(), provider.requiresSatellite()
//                            , provider.requiresCell(), provider.hasMonetaryCost(), provider.supportsAltitude(), provider.supportsSpeed()
//                            , provider.supportsBearing(), provider.getPowerRequirement(), provider.getAccuracy());
//                    mLocationManager.setTestProviderEnabled(providerStr, true);
//                    mLocationManager.setTestProviderStatus(providerStr, LocationProvider.AVAILABLE, null, System.currentTimeMillis());
                	mLocationManager.setTestProviderLocation(providerStr, mockLocation);
                }
            }
            if(mIsMock) mHandler.postDelayed(mRunnableMockLocation,50);
        }
    }

    private float mSysytemAccuracy = 0;
    private double mSysytemLat = 0;
    private double mSysytemLng = 0;
    
    /** 要模拟的地理位置 Provider (GPS/network/etc) */
    private List<String> mMockProviders = new ArrayList<String>();
    private List<String> mLocProviders = new ArrayList<String>();
    private void init() {
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mConnectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        mMockProviders.add(LocationManager.GPS_PROVIDER);
        // 基站定位无效！！！
        mMockProviders.add(LocationManager.NETWORK_PROVIDER);
        
        mLocProviders.addAll(mMockProviders);
        mLocProviders.add(LocationManager.PASSIVE_PROVIDER);
        
    }

    /** 
     * 通过GPS获取位置(获取不到位置，原因未知)
     */
    private void getLocation() {
        for (String providerStr : mLocProviders) {
        	Location location = mLocationManager.getLastKnownLocation(providerStr);
        	location = mLocationManager.getLastKnownLocation(providerStr);
        	if (location != null) {
        		Log.e(TAG, providerStr+"： "+location.toString());
        		mSysytemAccuracy = location.getAccuracy();
        		mSysytemLat = location.getLatitude();
        		mSysytemLng = location.getLongitude();
        	}else{
        		Log.e(TAG, providerStr+"定位失败！！！");
        	}
        }
    }

    private boolean mHasAddTestProvider = false;
    /** 设置虚拟位置 */
    private void setupMockLocation() {
        if (mHasAddTestProvider == false) {
            for (String providerStr : mMockProviders) {
                LocationProvider provider = mLocationManager.getProvider(providerStr);
                if (provider != null) {
                    mLocationManager.addTestProvider(provider.getName(), provider.requiresNetwork(), provider.requiresSatellite()
                            , provider.requiresCell(), provider.hasMonetaryCost(), provider.supportsAltitude(), provider.supportsSpeed()
                            , provider.supportsBearing(), provider.getPowerRequirement(), provider.getAccuracy());
                    mLocationManager.setTestProviderEnabled(providerStr, true);
                    mLocationManager.setTestProviderStatus(providerStr, LocationProvider.AVAILABLE, null, System.currentTimeMillis());
                    Log.e(TAG, "addTestProvider "+providerStr+" success.");
                }else{
                    Log.e(TAG, "addTestProvider "+providerStr+" failed.");
                }
               
            }
            // 模拟位置可用
            mHasAddTestProvider = true;
        }
    }

    private void stopMockLocation() {
        if (mHasAddTestProvider == true) {
            for (String providerStr : mMockProviders) {
                mLocationManager.removeTestProvider(providerStr);
            }
            // 模拟位置bu可用
            mHasAddTestProvider = false;
        }
    }

    /**
     * 虚拟位置是否可用
     * @return
     */
    private boolean isMockPositionEnabled() {
        // Android 6.0以下，通过Setting.Secure.ALLOW_MOCK_LOCATION判断
        // Android 6.0及以上，需要【选择模拟位置信息应用】，未找到方法，因此通过addTestProvider是否可用判断
        if(Build.VERSION.SDK_INT > 22){
            boolean isOk = true;
            try{
                mLocationManager.addTestProvider(LocationManager.GPS_PROVIDER, true, true, false, false, true, true, true, Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
            }catch(Exception e){
                Log.e(TAG, e.getLocalizedMessage());
                isOk = false;
            }
            return isOk;
        }else{
            return (Settings.Secure.getInt(getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION, 0) != 0);
        }
    }

    private long mPreviosTime = 0;

    @Override
    public void onBackPressed() {
        if ((System.currentTimeMillis() - mPreviosTime ) > 2000) {
            ToastUtil.show(mContext, "再按一次退出程序");
            mPreviosTime = System.currentTimeMillis();
        } else {
            finish();
            System.exit(0);
        }
    }

    @Override  
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    private String[] mPermissions = {"android.permission.ACCESS_MOCK_LOCATION"};
    private boolean checkPermissionsGranted() {
//        if(Build.VERSION.SDK_INT < 21) return true;;
//        int flag = 0;
//        for (int i = 0; i < mPermissions.length; i++) {
//            String p = mPermissions[i];
//            final boolean result = this.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED;
//            if (!result){
//                this.requestPermissions(new String[] { p }, 0);
//            }else{
//                flag++;
//            }
//        }
//        return flag == mPermissions.length;
    	return true;
    }

    private ConnectivityManager mConnectivityManager = null;

    @Override  
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
        
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        if(networkInfo != null && networkInfo.isAvailable()){
            Log.e(TAG,networkInfo.toString());
        }else{
            ToastUtil.show(mContext, "没有网络链接");
            this.startActivity(new Intent(Settings.ACTION_SETTINGS));
            return;
        }

        getLocation();

        //if(!checkPermissionsGranted()) return;

        if (isMockPositionEnabled()) {
            setupMockLocation();
        } else {
            ToastUtil.show(mContext, "请允许模拟位置");
            this.startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        }
    }

    @Override  
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        setVisibility(View.VISIBLE);
        if(requestCode == REQUEST_CODE_SEARCH && resultCode == RESULT_OK){
            LatLng l = data.getParcelableExtra("latlng");
            // 更新地图中心为搜索的点结果
            updateBaiduMapStatus(l.latitude,l.longitude,15);
        }
    }

    private ProgressDialog mLoadingDialog = null;
    private ProgressDialog showMapLoading(){
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("地图加载中(请确保网络链接)...");
        dialog.setCancelable(false);
        dialog.show();
        return dialog;
    }

    @Override
    public void onMapRenderFinished() {
        // 地图加载完成
        if(mLoadingDialog != null){
            mLoadingDialog.dismiss();
            mLoadingDialog = null;
        }
    }

}
