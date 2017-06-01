package questtest;

import android.content.Context;
import android.util.Log;

import com.sinovoice.hcicloudsdk.android.asr.recorder.ASRRecorder;
import com.sinovoice.hcicloudsdk.android.tts.player.TTSPlayer;
import com.sinovoice.hcicloudsdk.api.HciCloudSys;
import com.sinovoice.hcicloudsdk.common.AuthExpireTime;
import com.sinovoice.hcicloudsdk.common.HciErrorCode;
import com.sinovoice.hcicloudsdk.common.InitParam;
import com.sinovoice.hcicloudsdk.common.asr.AsrConfig;
import com.sinovoice.hcicloudsdk.common.asr.AsrInitParam;
import com.sinovoice.hcicloudsdk.common.asr.AsrRecogResult;
import com.sinovoice.hcicloudsdk.common.hwr.HwrInitParam;
import com.sinovoice.hcicloudsdk.common.tts.TtsConfig;
import com.sinovoice.hcicloudsdk.common.tts.TtsInitParam;
import com.sinovoice.hcicloudsdk.player.TTSCommonPlayer;
import com.sinovoice.hcicloudsdk.player.TTSPlayerListener;
import com.sinovoice.hcicloudsdk.recorder.ASRRecorderListener;
import com.sinovoice.hcicloudsdk.recorder.RecorderEvent;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * Created by li on 2017/3/26.
 */

public class VoiceFunc {

    private static final String TAG = "VoiceFunc";

    private AccountInfo mAccountInfo = null;
    private AccountInfo mAccountInfoTts = null;
    private ASRRecorder mAsrRecorder = null;
    private AsrConfig asrConfig = null;
    private TtsConfig ttsConfig = null;
    private TTSPlayer mTtsPlayer = null;
    private PlayerResult playerResult = null;
    private RecorderResult recorderResult = null;
    private String grammar = null;

    /**
     * 加载灵云账号信息
     *
     * @param filePath 灵云账号信息文件路径
     * @return AccountInfo 灵云账号信息类对象
     */
    private AccountInfo loadAccount(Context context, String filePath) {
        if(mAccountInfo == null) {
            mAccountInfo = new AccountInfo();
        }
        mAccountInfo.loadAccountInfo(context,filePath);
        return mAccountInfo;
    }

    /**
     * 加载灵云账号TTS信息
     *
     * @param filePath 灵云账号TTS信息文件路径
     * @return AccountInfo 灵云账号TTS信息类对象
     */
    private AccountInfo loadTtsAccount(Context context, String filePath) {
        if(mAccountInfoTts == null) {
            mAccountInfoTts = new AccountInfo();
        }
        mAccountInfoTts.loadAccountInfo(context,filePath);
        return mAccountInfoTts;
    }

    /**
     * 初始化灵云系统
     *
     * @param context 上下文信息,在Android平台下,必须为当前的Context
     * @param logPath 日志保存路径，可置为null
     * @return boolean 成功标志位
     */
    public boolean initSystem(Context context, String logPath, String filePath) {

        InitParam initParam = getInitParam(context, logPath, filePath);
        String strConfig = initParam.getStringConfig();

        // 灵云系统初始化
        int errCode = HciCloudSys.hciInit(strConfig, context);
        if (errCode != HciErrorCode.HCI_ERR_NONE && errCode != HciErrorCode.HCI_ERR_SYS_ALREADY_INIT) {
            Log.e(TAG,"系统初始化失败");
            Log.e(TAG,"hciInit error: " + HciCloudSys.hciGetErrorInfo(errCode));
            return false;
        }

        // 获取授权/更新授权文件 :
        errCode = checkAuthAndUpdateAuth();
        if (errCode != HciErrorCode.HCI_ERR_NONE) {
            // 由于系统已经初始化成功,在结束前需要调用方法hciRelease()进行系统的反初始化
            Log.e(TAG,"CheckAuthAndUpdateAuth error: " + HciCloudSys.hciGetErrorInfo(errCode));
            HciCloudSys.hciRelease();
            return false;
        }
        return true;
    }

    /**
     * 打开ASR录音机（其目标是将输入的语音信号转换为相应的文本或命令）
     */
    public void recordStart() {
        if (mAsrRecorder.getRecorderState() == ASRRecorder.RECORDER_STATE_IDLE) {
            asrConfig.addParam(AsrConfig.SessionConfig.PARAM_KEY_REALTIME, "yes");
            mAsrRecorder.start(asrConfig.getStringConfig(), grammar);
        } else {
            Log.e("recorder", "录音机未处于空闲状态，请稍等");
        }
    }

    /**
     * 结束录音或取消识别
     */
    public void recordCancle() {
        mAsrRecorder.cancel();
    }

    /**
     * 打开TTS播放器（可将任意文字信息实时转化为标准流畅的自然语音并朗读出来）
     * @param text 播放的文本信息
     */
    public void playStart(String text) {
        synth(text);
    }

    /**
     * 暂停播放
     */
    public void playPause() {
        mTtsPlayer.pause();
    }

    /**
     * 恢复播放
     */
    public void playResume() {
        mTtsPlayer.resume();
    }

    /**
     * 停止播放
     */
    public void playStop() {
        boolean bCanStop = mTtsPlayer.canStop();
        if (bCanStop) {
            mTtsPlayer.stop();
        }
    }

    /**
     * 反初始化，终止灵云系统
     */
    public void release() {
        if(mAsrRecorder != null) {
            mAsrRecorder.release();
        }
        if(mTtsPlayer != null) {
            mTtsPlayer.release();
        }
    }

    /**
     * 获取授权/更新授权文件
     *
     * @return int 获取授权返回码
     */
    private int checkAuthAndUpdateAuth() {
        // 获取系统授权到期时间
        int initResult;
        AuthExpireTime objExpireTime = new AuthExpireTime();
        initResult = HciCloudSys.hciGetAuthExpireTime(objExpireTime);
        if (initResult == HciErrorCode.HCI_ERR_NONE) {
            // 显示授权日期,如用户不需要关注该值,此处代码可忽略
            Date date = new Date(objExpireTime.getExpireTime() * 1000);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd",
                    Locale.CHINA);
            Log.i(TAG, "expire time: " + sdf.format(date));
            if (objExpireTime.getExpireTime() * 1000 > System.currentTimeMillis()) {
                // 已经成功获取了授权,并且距离授权到期有充足的时间(>7天)
                Log.i(TAG, "checkAuth success");
                return initResult;
            }
        }
        // 获取过期时间失败或者已经过期
        initResult = HciCloudSys.hciCheckAuth();
        if (initResult == HciErrorCode.HCI_ERR_NONE) {
            Log.i(TAG, "checkAuth success");
            return initResult;
        } else {
            Log.e(TAG, "checkAuth failed: " + initResult);
            return initResult;
        }
    }

    /**
     * 加载初始化信息
     *
     * @param context 上下文信息
     * @return InitParam 系统初始化参数
     */
    private InitParam getInitParam(Context context, String logPath, String filePath) {
        loadAccount(context,filePath);
        // 创建初始化参数辅助类
        InitParam initParam = new InitParam();

        // 授权文件所在路径，此项必填
        String authDirPath = context.getFilesDir().getAbsolutePath();
        initParam.addParam(InitParam.AuthParam.PARAM_KEY_AUTH_PATH, authDirPath);

        // 是否自动访问云授权,详见 获取授权/更新授权文件处注释
        initParam.addParam(InitParam.AuthParam.PARAM_KEY_AUTO_CLOUD_AUTH, "no");

        // 灵云云服务的接口地址，此项必填
        initParam.addParam(InitParam.AuthParam.PARAM_KEY_CLOUD_URL, mAccountInfo.getCloudUrl());

        // 开发者Key，此项必填，由捷通华声提供
        initParam.addParam(InitParam.AuthParam.PARAM_KEY_DEVELOPER_KEY, mAccountInfo.getDeveloperKey());

        // 应用Key，此项必填，由捷通华声提供
        initParam.addParam(InitParam.AuthParam.PARAM_KEY_APP_KEY, mAccountInfo.getAppKey());

        // 配置日志参数
        /*String sdcardState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(sdcardState)) { 
            String sdPath = Environment.getExternalStorageDirectory()
                    .getAbsolutePath();
            String packageName = context.getPackageName();

            String logPath = sdPath + File.separator + "sinovoice"
                    + File.separator + packageName + File.separator + "log"
                    + File.separator;*/

            // 日志文件地址
        if(logPath != null) {
            File fileDir = new File(logPath);
            if (!fileDir.exists()) {
                fileDir.mkdirs();
            }
        }

            // 日志的路径，可选，如果不传或者为空则不生成日志
            initParam.addParam(InitParam.LogParam.PARAM_KEY_LOG_FILE_PATH, logPath);

            // 日志数目，默认保留多少个日志文件，超过则覆盖最旧的日志
            initParam.addParam(InitParam.LogParam.PARAM_KEY_LOG_FILE_COUNT, "5");

            // 日志大小，默认一个日志文件写多大，单位为K
            initParam.addParam(InitParam.LogParam.PARAM_KEY_LOG_FILE_SIZE, "1024");

            // 日志等级，0=无，1=错误，2=警告，3=信息，4=细节，5=调试，SDK将输出小于等于logLevel的日志信息
            initParam.addParam(InitParam.LogParam.PARAM_KEY_LOG_LEVEL, "5");
        return initParam;
    }

    /**
     * 设置TTS（播放器）初始化参数
     *
     * @param context 上下文信息
     * @return boolean 成功标志位
     */
    public boolean initPlayer(Context context, String filePath) {
        playerResult = new PlayerResult();
        loadTtsAccount(context,filePath);
        // 读取用户的调用的能力
        String capKey = mAccountInfoTts.getCapKey();
        // 构造Tts初始化的帮助类的实例
        TtsInitParam ttsInitParam = new TtsInitParam();
        // 获取App应用中的lib的路径
        String dataPath = context.getFilesDir().getAbsolutePath().replace("files", "lib");
        ttsInitParam.addParam(TtsInitParam.PARAM_KEY_DATA_PATH, dataPath);
        // 此处演示初始化的能力为tts.cloud.xiaokun, 用户可以根据自己可用的能力进行设置, 另外,此处可以传入多个能力值,并用;隔开
        ttsInitParam.addParam(TtsInitParam.PARAM_KEY_INIT_CAP_KEYS, capKey);
        // 使用lib下的资源文件,需要添加android_so的标记
        ttsInitParam.addParam(HwrInitParam.PARAM_KEY_FILE_FLAG, "android_so");
        mTtsPlayer = new TTSPlayer();
        // 配置TTS初始化参数
        ttsConfig = new TtsConfig();
        mTtsPlayer.init(ttsInitParam.getStringConfig(), new TTSEventProcess());

        if (mTtsPlayer.getPlayerState() == TTSPlayer.PLAYER_STATE_IDLE) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 设置ASR（录音机）初始化参数
     *
     * @param context 上下文信息
     *
     */
    public void initRecorder(Context context) {
        recorderResult = new RecorderResult();
        String capKey = mAccountInfo.getCapKey();
        // 配置初始化参数
        mAsrRecorder = new ASRRecorder();
        AsrInitParam asrInitParam = new AsrInitParam();
        String dataPath = context.getFilesDir().getPath().replace("files", "lib");
        Log.i(TAG,"dataPath" + dataPath);
        asrInitParam.addParam(AsrInitParam.PARAM_KEY_INIT_CAP_KEYS, capKey);
        asrInitParam.addParam(AsrInitParam.PARAM_KEY_DATA_PATH, dataPath);
        asrInitParam.addParam(AsrInitParam.PARAM_KEY_FILE_FLAG, AsrInitParam.VALUE_OF_PARAM_FILE_FLAG_ANDROID_SO);
        Log.v(TAG, "init parameters:" + asrInitParam.getStringConfig());

        // 设置初始化参数
        mAsrRecorder.init(asrInitParam.getStringConfig(),
                new ASRResultProcess());

        // 配置识别参数
        asrConfig = new AsrConfig();
        // PARAM_KEY_CAP_KEY 设置使用的能力
        asrConfig.addParam(AsrConfig.SessionConfig.PARAM_KEY_CAP_KEY, capKey);
        // PARAM_KEY_AUDIO_FORMAT 音频格式根据不同的能力使用不用的音频格式
        asrConfig.addParam(AsrConfig.AudioConfig.PARAM_KEY_AUDIO_FORMAT,
                AsrConfig.AudioConfig.VALUE_OF_PARAM_AUDIO_FORMAT_PCM_16K16BIT);
        // PARAM_KEY_ENCODE 音频编码压缩格式，使用OPUS可以有效减小数据流量
        asrConfig.addParam(AsrConfig.AudioConfig.PARAM_KEY_ENCODE, AsrConfig.AudioConfig.VALUE_OF_PARAM_ENCODE_SPEEX);
        // 其他配置，此处可以全部选取缺省值
        asrConfig.addParam(AsrConfig.VadConfig.PARAM_KEY_VAD_HEAD, "0");
    }

    /**
     * TTS云端合成功能，不启用编码传输(默认encode=none)
     *
     * @param text 需要转为语音播放的文字
     */
    private void synth(String text) {
        // 读取用户的调用的能力
        String capKey = mAccountInfoTts.getCapKey();

        // 配置播放器的属性。包括：音频格式，音库文件，语音风格，语速等等。详情见文档。
        ttsConfig = new TtsConfig();
        // 音频格式
        ttsConfig.addParam(TtsConfig.BasicConfig.PARAM_KEY_AUDIO_FORMAT, "pcm16k16bit");
        // 指定语音合成的能力(云端合成,发言人是XiaoKun)
        ttsConfig.addParam(TtsConfig.SessionConfig.PARAM_KEY_CAP_KEY, capKey);
        // 设置合成语速
        ttsConfig.addParam(TtsConfig.BasicConfig.PARAM_KEY_SPEED, "5");
        // property为私有云能力必选参数，公有云传此参数无效
        ttsConfig.addParam("property", "cn_wangjing_common");

        if (mTtsPlayer.getPlayerState() == TTSCommonPlayer.PLAYER_STATE_PLAYING
                || mTtsPlayer.getPlayerState() == TTSCommonPlayer.PLAYER_STATE_PAUSE) {
            mTtsPlayer.stop();
        }

        if (mTtsPlayer.getPlayerState() == TTSCommonPlayer.PLAYER_STATE_IDLE) {
            mTtsPlayer.play(text, ttsConfig.getStringConfig());
        } else {
            Log.e(TAG,"播放器内部状态错误");
        }
        if (mTtsPlayer.getPlayerState() == TTSCommonPlayer.PLAYER_STOP_FLAG_PROMPT) {

        }
    }

    /**
     * TTS播放器监听器，用于接收播放器内部信息
     */
    private class TTSEventProcess implements TTSPlayerListener {

        // 出错回调，通知外部出现的错误
        @Override
        public void onPlayerEventPlayerError(TTSCommonPlayer.PlayerEvent playerEvent, int errorCode) {
            playerResult.setErrorCode(errorCode);
            playerResult.setState(playerEvent.name());
            Log.i(TAG, "onError " + playerEvent.name() + " code: " + errorCode);
        }

        // 进度回调，通知外部当前正在播放文本的起止位置
        @Override
        public void onPlayerEventProgressChange(TTSCommonPlayer.PlayerEvent playerEvent, int start, int end) {
            playerResult.setState(playerEvent.name());
            playerResult.setStartIndex(start);
            playerResult.setCurrentIndex(end);
            Log.i(TAG, "onProcessChange " + playerEvent.name() + " from "
                    + start + " to " + end);
        }

        // 事件回调，通知外部播放器状态的变化，如开始播放、播放完毕等
        @Override
        public void onPlayerEventStateChange(TTSCommonPlayer.PlayerEvent playerEvent) {
            playerResult.setState(playerEvent.name());
            Log.i(TAG, "onStateChange " + playerEvent.name());
        }
    }

    /**
     * ASR录音机监听器，用于接收录音机内部信息
     */
    private class ASRResultProcess implements ASRRecorderListener {

        // 出错回调，通知外部出现的错误
        @Override
        public void onRecorderEventError(RecorderEvent recorderEvent, int arg1) {
            recorderResult.setState(recorderEvent.name());
            recorderResult.setErrorCode(arg1);
            Log.e("recorder", "错误码为：" + arg1);
        }

        // 录音机识别完成事件回调
        @Override
        public void onRecorderEventRecogFinsh(RecorderEvent recorderEvent, AsrRecogResult arg1) {
            if(arg1 != null){
                if (arg1.getRecogItemList().size() > 0) {
                    String sResult = arg1.getRecogItemList().get(0).getRecogResult();
                    recorderResult.setState(recorderEvent.name());
                    recorderResult.setResult(sResult);
                    Log.e("recorder", "识别结果为：" + sResult);
                }
            }
        }

        // 录音机状态变化事件回调
        @Override
        public void onRecorderEventStateChange(RecorderEvent recorderEvent) {
            recorderResult.setState(recorderEvent.name());
            if(recorderEvent == RecorderEvent.RECORDER_EVENT_BEGIN_RECORD){
                Log.v("recorder","开始录音");
            } else if (recorderEvent == RecorderEvent.RECORDER_EVENT_BEGIN_RECOGNIZE){
                Log.v("recorder","开始识别");
            } else if (recorderEvent == RecorderEvent.RECORDER_EVENT_NO_VOICE_INPUT){
                Log.v("recorder","无音频输入");
            }
        }

        // 录音回调，通知外部当前的录音数据和音量大小
        @Override
        public void onRecorderRecording(byte[] voiceData, int volume) {
            recorderResult.setVoiceData(voiceData);
            recorderResult.setVolumn(volume);
            Log.v("recorder","当前音量大小为：" + volume);
        }

        // 进度回调
        @Override
        public void onRecorderEventRecogProcess(RecorderEvent recorderEvent, AsrRecogResult arg1) {
            recorderResult.setState(recorderEvent.name());
        }
    }
}
