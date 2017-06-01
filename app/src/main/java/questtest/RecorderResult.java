package questtest;

/**
 * Created by li on 2017/5/27.
 * ASR录音机返回的信息类
 */

public class RecorderResult {
    private String state;
    private String result;
    private int errorCode;
    private byte[] voiceData;
    private int volumn;

    /**
     * 获取ASR录音机当前状态，可能出现的事件：
     * RECORDER_EVENT_BEGIN_RECORD,RECORDER_EVENT_BEGIN_RECOGNIZE,RECORDER_EVENT_NO_VOICE_INPUT,
     * RECORDER_EVENT_PROGRESS,RECORDER_EVENT_DEVICE_ERROR,RECORDER_EVENT_ENGINE_ERROR
     *
     * @return 状态名称
     */
    public String getState() {
        return state;
    }

    /**
     * 获取录音过程中的错误码
     *
     * @return 错误码
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * 获取当前录音数据
     *
     * @return 录音数据
     */
    public byte[] getVoiceData() {
        return voiceData;
    }

    /**
     * 获取录音机识别的结果
     *
     * @return 识别结果
     */
    public String getResult() {
        return result;
    }

    /**
     * 获取当前录音音量大小
     *
     * @return 音量大小
     */
    public int getVolumn() {
        return volumn;
    }

    protected void setState(String state) {
        this.state = state;
    }

    protected void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }


    protected void setVoiceData(byte[] voiceData) {
        this.voiceData = voiceData;
    }


    protected void setVolumn(int volumn) {
        this.volumn = volumn;
    }

    protected void setResult(String result) {
        this.result = result;
    }
}
