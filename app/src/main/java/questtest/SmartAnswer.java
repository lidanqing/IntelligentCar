package questtest;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.httpclient.HttpException;

import java.io.File;
import java.lang.reflect.Type;

/**
 * Created by li on 2017/5/27.
 */

public class SmartAnswer {
    private String TAG = "SmartAnswer";
    private VoiceFunc voice;
    private RecorderResult recorderResult;
    private String query;
    private String queryShow;
    private String result;

    /**
     * 实时录音并回答问题
     *
     * @param filePath 灵云账号信息文件保存路径
     * @param ttsFilePath 灵云账号TTS信息文件保存路径
     * @param context 上下文信息
     * @param logPath 日志保存路径
     */
    public void answer(String filePath, String ttsFilePath, Context context, String logPath) {
        voice = new VoiceFunc();
        voice.initSystem(context,logPath,filePath);
        voice.initRecorder(context);
        voice.initPlayer(context,ttsFilePath);
        voice.recordStart();

        recorderResult = new RecorderResult();

            new Thread() {
                @Override
                public void run() {
                    Json jts = new Json();
                    while (true) {
                        result = recorderResult.getResult();
                        if (!"".equals(result)) {
                            try {
                                query = jts.ask(result);
                                if (query != null) {
                                    Log.i(TAG, "进入解析");
                                    Gson gson = new Gson();
                                    Type type = new TypeToken<JsonBean>() {
                                    }.getType();
                                    JsonBean jsonBean = gson.fromJson(result, type);
                                    System.out.println(jsonBean.getResult());
                                    queryShow = jsonBean.getSingleNode().getAnswerMsg();
                                    Log.d(TAG, "run: " + queryShow);
                                    Log.d(TAG, "Tts播放器开始播放");
                                    voice.playStart(queryShow);
                                }
                            } catch (HttpException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }.start();
        }

    public void releaseSys() {
        voice.release();
    }
}
