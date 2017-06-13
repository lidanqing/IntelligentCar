package liao;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.io.InputStream;

/**
 * GifView<br>
 * 本类可以显示一个gif动画，其使用方法和android的其它view（如imageview)一样。<br>
 * 如果要显示的gif太大，会出现OOM的问题。
 * @author liao
 *
 */
public class GifView extends View implements GifAction{


	public static int aa=0;
	/**gif解码器*/
	private GifDecoder gifDecoder = null;
	/**当前要画的帧的图*/
	private Bitmap currentImage = null;
	
	private boolean isRun = true;
	
	private boolean pause = false;
	
	private int showWidth = -1;
	private int showHeight = -1;
	private Rect rect = null;
	
	private DrawThread drawThread = null;
	
	private GifImageType animationType = GifImageType.SYNC_DECODER;
	
	/**
	 * 解码过程中，Gif动画显示的方式<br>
	 * 如果图片较大，那么解码过程会比较长，这个解码过程中，gif如何显示
	 * @author liao
	 *
	 */
	public enum GifImageType{
		WAIT_FINISH (0),
		SYNC_DECODER (1),
		COVER (2);
		
		GifImageType(int i){
			nativeInt = i;
		}
		final int nativeInt;
	}
	
	
	public GifView(Context context) {
        super(context);
        
    }
    
    public GifView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public GifView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }
    

    private void setGifDecoderImage(byte[] gif){
    	if(gifDecoder != null){
    		gifDecoder.free();
    		gifDecoder = null;
    	}
    	gifDecoder = new GifDecoder(gif,this);
    	gifDecoder.start();
    }

    private void setGifDecoderImage(InputStream is){
    	if(gifDecoder != null){
    		gifDecoder.free();
    		gifDecoder= null;
    	}
    	gifDecoder = new GifDecoder(is,this);
    	gifDecoder.start();
    }
    
    /**
     * 以字节数据形式设置gif图片
     * @param gif 图片
     */
    public void setGifImage(byte[] gif){
    	setGifDecoderImage(gif);
    }
    
    /**
     * 以字节流形式设置gif图片
     * @param is 图片
     */
    public void setGifImage(InputStream is){
    	setGifDecoderImage(is);
    }
    
    /**
     * 以资源形式设置gif图片
     * @param resId gif图片的资源ID
     */
    public void setGifImage(int resId){
    	Resources r = this.getResources();
    	InputStream is = r.openRawResource(resId);
    	setGifDecoderImage(is);
    }
    
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(gifDecoder == null)
        	return;
        if(currentImage == null){
        	currentImage = gifDecoder.getImage();
        }
        if(currentImage == null){
        		return;
        }
        int saveCount = canvas.getSaveCount();
        canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());
        if(showWidth == -1){
        	canvas.drawBitmap(currentImage, 0, 0,null);
        }else{
        	canvas.drawBitmap(currentImage, null, rect, null);
        }
        canvas.restoreToCount(saveCount);
    }
    
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	int pleft = getPaddingLeft();
        int pright = getPaddingRight();
        int ptop = getPaddingTop();
        int pbottom = getPaddingBottom();

        int widthSize;
        int heightSize;
        
        int w;
        int h;
        
        if(gifDecoder == null){
        	w = 1;
        	h = 1;
        }else{
        	w = gifDecoder.width;
        	h = gifDecoder.height;
        }
        
        w += pleft + pright;
        h += ptop + pbottom;
            
        w = Math.max(w, getSuggestedMinimumWidth());
        h = Math.max(h, getSuggestedMinimumHeight());

        widthSize = resolveSize(w, widthMeasureSpec);
        heightSize = resolveSize(h, heightMeasureSpec);
        
        setMeasuredDimension(widthSize, heightSize);
    }
    
    /**
     * 只显示第一帧图片<br>
     * 调用本方法后，gif不会显示动画，只会显示gif的第一帧图
     */
    public void showCover(){
    	if(gifDecoder == null)
    		return;
    	pause = true;
    	currentImage = gifDecoder.getImage();
    	invalidate();
    }
    

    public void showAnimation(){
    	if(pause){
    		pause = false;
    	}
    }
    

    public void setGifImageType(GifImageType type){
    	if(gifDecoder == null)
    		animationType = type;
    }
    
    /**
     * 设置要显示的图片的大小<br>
     * 当设置了图片大小 之后，会按照设置的大小来显示gif（按设置后的大小来进行拉伸或压缩）
     * @param width 要显示的图片宽
     * @param height 要显示的图片高
     */
    public void setShowDimension(int width,int height){
    	if(width > 0 && height > 0){
	    	showWidth = width;
	    	showHeight = height;
	    	rect = new Rect();
			rect.left = 0;
			rect.top = 0;
			rect.right = width;
			rect.bottom = height;
    	}
    }
    
    public void parseOk(boolean parseStatus,int frameIndex){
    	if(parseStatus){
    		if(gifDecoder != null){
    			switch(animationType){
    			case WAIT_FINISH:
    				if(frameIndex == -1){
    					if(gifDecoder.getFrameCount() > 1){
    	    				DrawThread dt = new DrawThread();
							aa++;
							Log.e("gif","aa="+aa);
    	    	    		dt.start();
    	    			}else{
    	    				reDraw();
    	    			}
    				}
    				break;
    			case COVER:
    				if(frameIndex == 1){
    					currentImage = gifDecoder.getImage();
    					reDraw();
    				}else if(frameIndex == -1){
    					if(gifDecoder.getFrameCount() > 1){
    						if(drawThread == null){
        						drawThread = new DrawThread();
								aa++;
								Log.e("gif","aa="+aa);
        						drawThread.start();
        					}
    					}else{
    						reDraw();
    					}
    				}
    				break;
    			case SYNC_DECODER:
    				if(frameIndex == 1){
    					currentImage = gifDecoder.getImage();
    					reDraw();
    				}else if(frameIndex == -1){
    					reDraw();
    				}else{
    					if(drawThread == null){
    						drawThread = new DrawThread();
							aa++;
							Log.e("gif","aa="+aa);
    						drawThread.start();
    					}
    				}
    				break;
    			}
 
    		}else{
    			Log.e("gif","parse error");
    		}
    		
    	}
    }
    
    private void reDraw(){
    	if(redrawHandler != null){
			Message msg = redrawHandler.obtainMessage();
			redrawHandler.sendMessage(msg);
    	}
    }
    
    private Handler redrawHandler = new Handler(){
    	public void handleMessage(Message msg) {
    		invalidate();
    	}
    };


	private class DrawThread extends Thread{
		public void run(){
			if(gifDecoder == null){
				return;
			}
			while(isRun){
				if (pause == false||gifDecoder.getFrameCount() == 1) {
					try {
						GifFrame frame = gifDecoder.next();
						if (frame == null) {
							SystemClock.sleep(50);
							continue;
						}
						if (frame.image != null)
							currentImage = frame.image;
						else if (frame.imageName != null) {
							currentImage = BitmapFactory.decodeFile(frame.imageName);
						}
						long sp = frame.delay;
						if (redrawHandler != null) {
							reDraw();
							SystemClock.sleep(sp);
						} else {
							break;
						}
					}catch (Exception e)
					{
						Log.d("TAG", "run: gifdecoder.next error");
					}
				} else {
					SystemClock.sleep(50);
				}
			}
		}
	}

}
