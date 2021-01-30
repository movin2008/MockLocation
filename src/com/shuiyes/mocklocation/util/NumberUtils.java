package com.shuiyes.mocklocation.util;

import java.util.Random;

public class NumberUtils {

    /**
     * 小数点后面7位格式化
     * @param value
     * @return
     */
    public static String numFormat(double value){
        return new java.text.DecimalFormat("#.000000").format(value);
    }

    /**
     * 
     * @return  3,4
     */
    public static int random(){
        return new Random().nextInt(2)+3;
    }

    public static int random100(){
        return new Random().nextInt(100);
    }

    
}
