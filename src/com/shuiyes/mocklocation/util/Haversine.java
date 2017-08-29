package com.shuiyes.mocklocation.util;

import com.baidu.mapapi.model.LatLng;

public class Haversine {

    static double EARTH_RADIUS = 6378137.0;//km 地球半径 平均值，米
    static double X_PI = 3.14159265358979324 * 3000.0 / 180.0;

    // 返回单位是米
    public static double Distance(double lat1, double lng1, double lat2, double lng2) {

        //用haversine公式计算球面两点间的距离。
        //经纬度转换成弧度
        lat1 = ConvertDegreesToRadians(lat1);
        lng1 = ConvertDegreesToRadians(lng1);
        lat2 = ConvertDegreesToRadians(lat2);
        lng2 = ConvertDegreesToRadians(lng2);

        //差值
        double vLat = Math.abs(lat1 - lat2);
        double vlng = Math.abs(lng1 - lng2);
        
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(vLat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(vlng / 2), 2)));
        s = s * EARTH_RADIUS;
        s = Math.round(s * 10000) / 10000;
        return s;
    }

    // two point's distance
    public static double distance(double latA, double lngA, double latB, double lngB){
        double earthR = 6371000;
        double x = Math.cos(latA * Math.PI / 180.) * Math.cos(latB * Math.PI / 180.) * Math.cos((lngA - lngB) * Math.PI / 180);
        double y = Math.sin(latA * Math.PI / 180.) * Math.sin(latB * Math.PI / 180.);
        double s = x + y;
        if (s > 1) s = 1;
        if (s < -1) s = -1;
        double alpha = Math.acos(s);
        double distance = alpha * earthR;
        distance = Math.round(distance * 10000) / 10000;
        return distance;
    }

     /// 将角度换算为弧度。
     private static double ConvertDegreesToRadians(double degrees){
         return degrees * Math.PI / 180;
     }

     private static double ConvertRadiansToDegrees(double radian){
         return radian * 180.0 / Math.PI;
     }

    //BD-09 to GCJ-02
    public static double[] bd09togcj02(double bdLat, double bdlng) {
        double x = bdlng - 0.0065,y = bdLat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * X_PI);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * X_PI);
        double gcjLat = z * Math.sin(theta),gcjlng = z * Math.cos(theta);
        double d[] = { gcjLat, gcjlng };
        return d;
    }

    //GCJ-02 to BD-09
    public double[] gcj02tobd09(double gcjLat, double gcjLon) {
        double x = gcjLon,y = gcjLat;  
        double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * X_PI);  
        double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * X_PI);  
        double bdLon = z * Math.cos(theta) + 0.0065;  
        double bdLat = z * Math.sin(theta) + 0.006; 
        double d[] = { bdLat, bdLon };
        return d;
    }

    //WGS-84 to GCJ-02
    public static double[] wgs84togcj02(double wgsLat, double wgslng) {
        if (outOfChina(wgsLat, wgslng)){
            double d[] = { wgsLat, wgslng };
            return d;
        }

        double[] r = delta(wgsLat, wgslng);
        double d[] = { wgsLat + r[0], wgslng + r[1] };
        return d;
    }

    //GCJ-02 to WGS-84 exactly
    public static double[] gcj02towgs84_exactly(double gcjLat, double gcjlng) {
        double initDelta = 0.01;
        double threshold = 0.000000001;
        double dLat = initDelta, dlng = initDelta;
        double mLat = gcjLat - dLat,mlng = gcjlng - dlng;
        double pLat = gcjLat + dLat,plng = gcjlng + dlng;
        double wgsLat = 0,wgslng = 0,i = 0;
        while (true) {
            wgsLat = (mLat + pLat) / 2;
            wgslng = (mlng + plng) / 2;
            double[] tmp = wgs84togcj02(wgsLat, wgslng);
            dLat = tmp[0] - gcjLat;
            dlng = tmp[1] - gcjlng;
            if ((Math.abs(dLat) < threshold) && (Math.abs(dlng) < threshold))
                break;
 
            if (dLat > 0) pLat = wgsLat; else mLat = wgsLat;
            if (dlng > 0) plng = wgslng; else mlng = wgslng;
 
            if (++i > 10000) break;
        }

        double d[] = { wgsLat, wgslng };
        return d;
    }

   //GCJ-02 to WGS-84
    public double[] gcj02towgs84(double gcjLat, double gcjLon) {
        if (outOfChina(gcjLat, gcjLon)){
            double d[] = { gcjLat, gcjLat };
            return d;
        }
         
        double[] r = delta(gcjLat, gcjLon);
        double d[] = { gcjLat - r[0], gcjLon - r[1] };
        return d;
    }

    private static double[] delta(double lat, double lng){
        // Krasovsky 1940
        //
        // a = 6378245.0, 1/f = 298.3
        // b = a * (1 - f)
        // ee = (a^2 - b^2) / a^2;
        double a = 6378245.0;//  a: 卫星椭球坐标投影到平面地图坐标系的投影因子。
        double ee = 0.00669342162296594323;//  ee: 椭球的偏心率。
        double dLat = transformLat(lng - 105.0, lat - 35.0);
        double dlng = transformlng(lng - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * Math.PI;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * Math.PI);
        dlng = (dlng * 180.0) / (a / sqrtMagic * Math.cos(radLat) * Math.PI);
        double d[] = { dLat, dlng };
        return d;
    }

    private static boolean outOfChina(double lat, double lng){
        if (lng < 72.004 || lng > 137.8347)
            return true;
        if (lat < 0.8293 || lat > 55.8271)
            return true;
        return false;
    }

    private static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin(y / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * Math.PI) + 320 * Math.sin(y * Math.PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private static double transformlng(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin(x / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * Math.PI) + 300.0 * Math.sin(x / 30.0 * Math.PI)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * 百度坐标转GPS坐标
     * @param bd09
     * @return
     */
    public static LatLng convertBaiduToGPS(LatLng bd09) {
        double[] gcj02 = Haversine.bd09togcj02(bd09.latitude, bd09.longitude);
        double[] wgs84 = Haversine.gcj02towgs84_exactly(gcj02[0], gcj02[1]);
        LatLng latlng = new LatLng(wgs84[0],wgs84[1]);
        return latlng;
    }
    
}
