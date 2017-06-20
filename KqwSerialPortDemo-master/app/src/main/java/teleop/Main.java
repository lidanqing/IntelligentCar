package teleop;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ioio.lib.api.exception.ConnectionLostException;


public class Main extends SerialPortActivity {
	EditText etxtIP, etxtPass;
	Spinner spinnerSize;
	SeekBar barQuality;
	Dialog dialogConnect;
	Button btnController, btnIOIO;
	Dialog dialog;
	
	TextView txtQuality;
	ArrayList<String> size;
	List<Camera.Size> previewSize;
	Button buttonOK;

    Camera mCamera;
	int cameraSize = 0;
	int count = 0;
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	    getWindow().setFormat(PixelFormat.RGBA_8888);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
        		| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		overridePendingTransition(0, 0);
        setContentView(R.layout.main);
        btnController = (Button)findViewById(R.id.btnController);
        btnController.setVisibility(View.INVISIBLE);
        btnController.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		Intent intent = new Intent(Main.this, ControllerConnection.class);
        		startActivity(intent);
        	}
        });

        btnIOIO = (Button)findViewById(R.id.btnIOIO);
		btnIOIO.setVisibility(View.INVISIBLE);
        btnIOIO.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		Intent intent = new Intent(Main.this, IOIOConnection.class);
        		startActivity(intent);
        	}
        });
        
        buttonFadeIn();
        
    }
    
    public void buttonFadeIn() {
    	Handler refresh = new Handler(Looper.getMainLooper());
        refresh.post(new Runnable() {
			public void run()
			{
		        try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		        Animation aa = new TranslateAnimation(Animation.RELATIVE_TO_SELF, (float)0
						, Animation.RELATIVE_TO_SELF, (float)0
						, Animation.RELATIVE_TO_SELF, (float)-1.2
						, Animation.RELATIVE_TO_SELF, (float)0);
				aa.setDuration(1000);
				aa.setInterpolator(new DecelerateInterpolator());
				btnController.startAnimation(aa);
				btnController.setVisibility(View.VISIBLE);
				
		        aa = new TranslateAnimation(Animation.RELATIVE_TO_SELF, (float)0
						, Animation.RELATIVE_TO_SELF, (float)0
						, Animation.RELATIVE_TO_SELF, (float)1.2
						, Animation.RELATIVE_TO_SELF, (float)0);
				aa.setDuration(1000);
				aa.setInterpolator(new DecelerateInterpolator());
				btnIOIO.startAnimation(aa);
				btnIOIO.setVisibility(View.VISIBLE);
			}
		});	        
	}

	@Override
	protected void onDataReceived(final byte[] buffer, final int size) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				String kk=new String(buffer).substring(0,size);
				//Toast.makeText(mApplication, "收到消息：" + " "+kk+" " +"  size = " + size, Toast.LENGTH_SHORT).show();
				Log.e("receive data",kk);
				count++;
				if(count > 4) {
					String[] str = kk.split("\\|");
					for(String s : str) {
						Log.e("str",s );
					}

					IOIOu.wendu = str[0];
					Log.e("wendu",IOIOu.wendu );
					IOIOu.qiya = str[1];
					IOIOu.haiba = str[2];
					IOIOu.shidu = str[3];
					IOIOu.qiti = str[4];
					IOIOu.guangqiang = str[5];
					if(count > 10) {
						if (Float.parseFloat(IOIOu.guangqiang) < 50) {
							try {
								IOIO_thread.led.write(true);
							} catch (ConnectionLostException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		});
	}
}
