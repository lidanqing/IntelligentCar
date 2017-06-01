package questtest;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ${kang} on 2016/6/20.
 */

public class AccountInfo {
    private static AccountInfo mInstance;

    private Map<String, String> mAccountMap;

    FileReader fb = null;

    public AccountInfo() {
        mAccountMap = new HashMap<String, String>();
    }

    /*public static AccountInfo getInstance() {
        if (mInstance == null) {
            mInstance = new AccountInfo();
        }
        return mInstance;
    }*/

    public String getCapKey(){
        return mAccountMap.get("capKey");
    }
    public String getDeveloperKey(){
        return mAccountMap.get("developerKey");
    }
    public String getAppKey(){
        return mAccountMap.get("appKey");
    }
    public String getCloudUrl(){
        return mAccountMap.get("cloudUrl");
    }

    /**
     * 加载用户的注册信息
     *
     * @param filePath
     */
    public boolean loadAccountInfo(Context context, String filePath) {
        boolean isSuccess = true;
        try {
            InputStream in = null;
            /*fb = new FileReader(filePath);
            *//*in = context.getResources().getAssets().open("AccountInfo.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(in,
                    "utf-8");*//*
            BufferedReader br = new BufferedReader(fb);*/
            in = context.getResources().getAssets().open(filePath);
            InputStreamReader inputStreamReader = new InputStreamReader(in,
                    "utf-8");
            BufferedReader br = new BufferedReader(inputStreamReader);
            String temp = null;
            String[] sInfo = new String[2];
            temp = br.readLine();
            while (temp != null) {
                if (!temp.startsWith("#") && !temp.equalsIgnoreCase("")) {
                    sInfo = temp.split("=");
                    if (sInfo.length == 2){
                        if(sInfo[1] == null || sInfo[1].length() <= 0){
                            isSuccess = false;
                            Log.e("AccountInfo", sInfo[0] + "is null");
                            break;
                        }
                        mAccountMap.put(sInfo[0], sInfo[1]);
                    }
                }
                temp = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            isSuccess = false;
        }

        return isSuccess;
    }
}
