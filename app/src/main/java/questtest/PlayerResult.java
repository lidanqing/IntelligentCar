package questtest;

/**
 * Created by li on 2017/5/27.
 */

public class PlayerResult {
    private String state;
    private int startIndex;
    private int currentIndex;
    private int errorCode;

    /**
     * 获取当前正在播放文本的起始位置
     *
     * @return 起始位置
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * 获取当前正在播放文本的进度位置
     *
     * @return 进度位置
     */
    public int getStartIndex() {
        return startIndex;
    }

    /**
     * 获取TTS播放器当前状态，可能出现的事件：
     * PLAYER_EVENT_BEGIN: 开始播放,PLAYER_EVENT_BUFFERING: 播放缓冲，等待合成数据,
     * PLAYER_EVENT_BUFFERING_END: 缓冲结束，继续播放,PLAYER_EVENT_PAUSE: 暂停播放,
     * PLAYER_EVENT_RESUME: 恢复播放,PLAYER_EVENT_END: 播放完毕
     * PLAYER_EVENT_PROGRESS：正在播放,PLAYER_EVENT_DEVICE_ERROR,PLAYER_EVENT_ENGINE_ERROR
     *
     * @return 状态名称
     */
    public String getState() {
        return state;
    }

    /**
     * 获取播放过程中的错误码
     *
     * @return 错误码
     */
    public int getErrorCode() {
        return errorCode;
    }

    protected void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    protected void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    protected void setState(String state) {
        this.state = state;
    }

    protected void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
}
