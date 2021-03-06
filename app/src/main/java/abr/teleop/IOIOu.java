package abr.teleop;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.IOIOLooperProvider;
import ioio.lib.util.android.IOIOActivity;
import ioio.lib.util.android.IOIOAndroidApplicationHelper;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class IOIOu extends Activity implements Callback, PreviewCallback, PictureCallback,IOIOLooperProvider/*,CameraBridgeViewBase.CvCameraViewListener2*/ {
	private static final String TAG_IOIO = "CameraRobot-IOIO";
	private static final String TAG_CAMERA = "CameraRobot-Camera";



	public static final int DIRECTION_STOP = 10;
	public static final int DIRECTION_UP = 11;
	public static final int DIRECTION_UPRIGHT = 12;
	public static final int DIRECTION_RIGHT = 13;
	public static final int DIRECTION_DOWNRIGHT = 14;
	public static final int DIRECTION_DOWN = 15;
	public static final int DIRECTION_DOWNLEFT = 16;
	public static final int DIRECTION_LEFT = 17;
	public static final int DIRECTION_UPLEFT = 18;

	int direction_state = DIRECTION_STOP;
	int direction_PT_state = DIRECTION_STOP;

	static final int DEFAULT_PWM = 1500, MAX_PWM = 2000, MIN_PWM = 1000, PWM_STEP=10, K1 = 3, K2=1, K3=10;

	RelativeLayout layoutPreview;
	TextView txtspeed_motor, txtIP;
	Button buttonUp, buttonUpLeft, buttonUpRight, buttonDown
			, buttonDownLeft, buttonDownRight, buttonRight, buttonLeft;
	String TAG = "IOIOu";

	int speed_motor = 0;
	static int pwm_pan, pwm_tilt;
	static int pwm_speed, pwm_steering;

	Camera mCamera;
	Camera.Parameters params;
	SurfaceView mPreview;
	int startTime = 0;

	IOIOService ioio;
	OutputStream out;
	DataOutputStream dos;

	OrientationEventListener oel;
	OrientationManager om;

	int size, quality;
	String pass;
	boolean connect_state = false;

	Bitmap bitmap;
	ByteArrayOutputStream bos;
	int w, h;
	int[] rgbs;
	boolean initialed = false;

	String wendu = "19";
	String tempra = "t" + wendu;

	String shidu = "31";
	String humidity = "h" + shidu;

	private final IOIOAndroidApplicationHelper helper_ = new IOIOAndroidApplicationHelper(this, this);
	ToggleButton toggleButton_;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		helper_.create();
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		setContentView(R.layout.ioio);

		toggleButton_ = (ToggleButton)findViewById(R.id.ToggleButton);
		enableUi(false);

		pass = getIntent().getExtras().getString("Pass");
		size = getIntent().getExtras().getInt("Size");
		quality = getIntent().getExtras().getInt("Quality");

		buttonUp = (Button)findViewById(R.id.buttonUp);
		buttonUpLeft = (Button)findViewById(R.id.buttonUpLeft);
		buttonUpRight = (Button)findViewById(R.id.buttonUpRight);
		buttonDown = (Button)findViewById(R.id.buttonDown);
		buttonDownLeft = (Button)findViewById(R.id.buttonDownLeft);
		buttonDownRight = (Button)findViewById(R.id.buttonDownRight);
		buttonRight = (Button)findViewById(R.id.buttonRight);
		buttonLeft = (Button)findViewById(R.id.buttonLeft);

		txtspeed_motor = (TextView)findViewById(R.id.txtSpeed);

		txtIP = (TextView)findViewById(R.id.txtIP);
		txtIP.setText(getIP());

		mPreview = (SurfaceView)findViewById(R.id.preview);
		mPreview.getHolder().addCallback(this);
		mPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		ioio = new IOIOService(getApplicationContext(), mHandler, pass);
		ioio.execute();

		layoutPreview = (RelativeLayout)findViewById(R.id.layoutPreview);
		layoutPreview.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(mCamera != null)
					mCamera.autoFocus(null);
			}
		});

		om = new OrientationManager(this);
	}

	public void enableUi(final boolean enable)
	{
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				toggleButton_.setEnabled(enable);
			}
		});
	}

	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			int command = msg.what;

			clearCheckBox();
			if(command == IOIOService.MESSAGE_PASS) {
				try {
					out = ((Socket)msg.obj).getOutputStream();
					dos = new DataOutputStream(out);
					connect_state = true;
					sendString("ACCEPT");
					Log.i(TAG_IOIO, "Connect");
				} catch (IOException e) {
					Log.e(TAG_IOIO, e.toString());
				}
			} else if(command == IOIOService.MESSAGE_WRONG) {
				try {
					out = ((Socket)msg.obj).getOutputStream();
					dos = new DataOutputStream(out);
					sendString("WRONG");
					ioio.killTask();
					new Handler().postDelayed(new Runnable() {
						public void run() {
							ioio = new IOIOService(getApplicationContext(), mHandler, pass);
							ioio.execute();
						}
					}, 1000);
				} catch (IOException e) {
					Log.e(TAG_IOIO, e.toString());
				}
			} else if(command == IOIOService.MESSAGE_DISCONNECTED) {
				Toast.makeText(getApplicationContext()
						, "Server down, willbe restart service in 1 seconds"
						, Toast.LENGTH_SHORT).show();
				ioio.killTask();
				new Handler().postDelayed(new Runnable() {
					public void run() {
						ioio = new IOIOService(getApplicationContext(), mHandler, pass);
						ioio.execute();
					}
				}, 1000);
			} else if(command == IOIOService.MESSAGE_CLOSE) {
				Log.e(TAG_IOIO, "Close");
				connect_state = false;
				ioio.killTask();
				new Handler().postDelayed(new Runnable() {
					public void run() {
						ioio = new IOIOService(getApplicationContext(), mHandler, pass);
						ioio.execute();
					}
				}, 1000);
			} else if(command == IOIOService.MESSAGE_FLASH) {
				Log.e("Check", "111");
				Log.e("Check", msg.obj.toString());
				Log.e("Check", "111");
				if(params.getSupportedFlashModes() != null) {
					if(msg.obj.toString().equals("LEDON")) {
						params.setFlashMode(Parameters.FLASH_MODE_TORCH);
					} else if(msg.obj.toString().equals("LEDOFF")) {
						params.setFlashMode(Parameters.FLASH_MODE_OFF);
					}
				} else {
					sendString("NoFlash");
				}
				mCamera.setParameters(params);
			} else if(command == IOIOService.MESSAGE_SNAP) {
				if((int)(System.currentTimeMillis() / 1000) - startTime > 1) {
					Log.d(TAG_CAMERA,"Snap");
					startTime = (int) (System.currentTimeMillis() / 1000);
					mCamera.takePicture(null, null, null, IOIOu.this);
				}
			} else if(command == IOIOService.MESSAGE_FOCUS) {
				mCamera.autoFocus(null);
			}
			else if(command == IOIOService.MESSAGE_MOVE)
			{
				pwm_speed = msg.arg1;
				pwm_steering = msg.arg2;
				txtspeed_motor.setText("speed:" + String.valueOf(pwm_speed) + "steering:" + String.valueOf(pwm_steering)
						+ "pan:" + String.valueOf(pwm_pan) + "tilt:" + String.valueOf(pwm_tilt));
				//Log.e("IOIO", "pwm_speed: " + pwm_speed + " pwm_steering: " + pwm_steering);
			}
			else if(command == IOIOService.MESSAGE_STOP)
			{
				pwm_speed = 1500;
				pwm_steering = 1500;
				txtspeed_motor.setText("speed:" + String.valueOf(pwm_speed) + "steering:" + String.valueOf(pwm_steering)
				+ "pan:" + String.valueOf(pwm_pan) + "tilt:" + String.valueOf(pwm_tilt));
			}
			else if(command == IOIOService.MESSAGE_PT_MOVE)
			{
				pwm_pan = msg.arg1;
				pwm_tilt = msg.arg2;
				txtspeed_motor.setText("speed:" + String.valueOf(pwm_speed) + "steering:" + String.valueOf(pwm_steering)
						+ "pan:" + String.valueOf(pwm_pan) + "tilt:" + String.valueOf(pwm_tilt));
				//Log.e("IOIO", "pwm_pan: " + pwm_pan + " pwm_tilt: " + pwm_tilt);
			}
			else if(command == IOIOService.MESSAGE_PT_STOP)
			{
				pwm_pan = 1500;
				pwm_tilt = 1500;
				txtspeed_motor.setText("speed:" + String.valueOf(pwm_speed) + "steering:" + String.valueOf(pwm_steering)
						+ "pan:" + String.valueOf(pwm_pan) + "tilt:" + String.valueOf(pwm_tilt));
			}
			else if(command == IOIOService.MESSAGE_TEMPERA)
			{
				try {
					out = ((Socket)msg.obj).getOutputStream();
					dos = new DataOutputStream(out);
					sendString(tempra);
					Log.e(TAG_IOIO, "Tempera");
				} catch (IOException e) {
					Log.e(TAG_IOIO, e.toString());
				}
			}
			else if(command == IOIOService.MESSAGE_HUMIDITY)
			{
				try {
					out = ((Socket)msg.obj).getOutputStream();
					dos = new DataOutputStream(out);
					sendString(humidity);
					Log.e(TAG_IOIO, "Humidity");
				} catch (IOException e) {
					Log.e(TAG_IOIO, e.toString());
				}
			}
			else if(command == IOIOService.MESSAGE_AUTO) {
				Toast.makeText(getApplicationContext()
						, "AUTO"
						, Toast.LENGTH_SHORT).show();
				//ioio.killTask();
				Log.e(TAG,"Auto2");
				Intent intent = new Intent(getApplicationContext(),Main_activity.class);
				startActivity(intent);
			}
		}
	};

	public void onPause() {
		super.onPause();
		ioio.killTask();
		finish();
	}

	public void clearCheckBox() {
		buttonUp.setPressed(false);
		buttonUpLeft.setPressed(false);
		buttonUpRight.setPressed(false);
		buttonDown.setPressed(false);
		buttonDownLeft.setPressed(false);
		buttonDownRight.setPressed(false);
		buttonRight.setPressed(false);
		buttonLeft.setPressed(false);
	}

	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		if (mPreview == null)
			return;

		try {
			mCamera.stopPreview();
		} catch (Exception e){ }

		params = mCamera.getParameters();
		Camera.Size pictureSize = getMaxPictureSize(params);
		Camera.Size previewSize = params.getSupportedPreviewSizes().get(size);

		params.setPictureSize(pictureSize.width, pictureSize.height);
		params.setPreviewSize(previewSize.width, previewSize.height);
		params.setPreviewFrameRate(getMaxPreviewFps(params));

		/*Display display = getWindowManager().getDefaultDisplay();
		LayoutParams lp = layoutPreview.getLayoutParams();

		if(om.getOrientation() == OrientationManager.LANDSCAPE_NORMAL
				|| om.getOrientation() == OrientationManager.LANDSCAPE_REVERSE) {
			float ratio = (float)previewSize.width / (float)previewSize.height;
			if((int)((float)mPreview.getWidth() / ratio) >= display.getHeight()) {
				lp.height = (int)((float)mPreview.getWidth() / ratio);
				lp.width = mPreview.getWidth();
			} else {
				lp.height = mPreview.getHeight();
				lp.width = (int)((float)mPreview.getHeight() * ratio);
			}
		} else if(om.getOrientation() == OrientationManager.PORTRAIT_NORMAL
				|| om.getOrientation() == OrientationManager.PORTRAIT_REVERSE) {
			float ratio = (float)previewSize.height / (float)previewSize.width;
			if((int)((float)mPreview.getWidth() / ratio) >= display.getHeight()) {
				lp.height = (int)((float)mPreview.getWidth() / ratio);
				lp.width = mPreview.getWidth();
			} else {
				lp.height = mPreview.getHeight();
				lp.width = (int)((float)mPreview.getHeight() * ratio);
			}
		}

		layoutPreview.setLayoutParams(lp);
		int deslocationX = (int) (lp.width / 2.0 - mPreview.getWidth() / 2.0);
		layoutPreview.animate().translationX(-deslocationX);*/

		params.setJpegQuality(100);
		mCamera.setParameters(params);
		mCamera.setPreviewCallback(this);

		switch(om.getOrientation()) {
			case OrientationManager.LANDSCAPE_NORMAL:
				mCamera.setDisplayOrientation(0);
				break;
			case OrientationManager.PORTRAIT_NORMAL:
				mCamera.setDisplayOrientation(90);
				break;
			case OrientationManager.LANDSCAPE_REVERSE:
				mCamera.setDisplayOrientation(180);
				break;
			case OrientationManager.PORTRAIT_REVERSE:
				mCamera.setDisplayOrientation(270);
				break;
		}

		try {
			mCamera.setPreviewDisplay(mPreview.getHolder());
			mCamera.startPreview();
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	public void surfaceCreated(SurfaceHolder arg0) {
		try {
			mCamera = Camera.open(0);
			mCamera.setPreviewDisplay(arg0);
			//mCamera.startPreview();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void surfaceDestroyed(SurfaceHolder arg0) {
		mCamera.setPreviewCallback(null);
		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;
	}

	public void onPictureTaken(byte[] arg0, Camera arg1) {
		Log.d(TAG_CAMERA, "onPictureTaken");
		int imageNum = 0;
		File imagesFolder = new File(Environment.getExternalStorageDirectory(), "DCIM/CameraRemote");
		imagesFolder.mkdirs();

		SimpleDateFormat sd = new SimpleDateFormat("yyyyMMdd-hhmmss");
		String date = sd.format(new Date());

		String fileName = "IMG_" + date + ".jpg";
		File output = new File(imagesFolder, fileName);
		while (output.exists()){
			imageNum++;
			fileName = "IMG_" + date + "_" + String.valueOf(imageNum) + ".jpg";
			output = new File(imagesFolder, fileName);
		}

		Log.i(TAG_CAMERA,output.toString());

		try {
			FileOutputStream fos = new FileOutputStream(output);
			fos.write(arg0);
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Log.d(TAG_CAMERA,"Restart Preview");
		mCamera.stopPreview();
		mCamera.setPreviewCallback(this);
		mCamera.startPreview();
		sendString("Snap");
	}

	public void onPreviewFrame(final byte[] arg0, Camera arg1) {
		if(!initialed) {
			w = mCamera.getParameters().getPreviewSize().width;
			h = mCamera.getParameters().getPreviewSize().height;
			rgbs = new int[w * h];
			initialed = true;
		}

		if(arg0 != null && connect_state) {
			try {
				decodeYUV420(rgbs, arg0, w, h);
				bitmap = Bitmap.createBitmap(rgbs, w, h, Config.ARGB_8888);
				bos = new ByteArrayOutputStream();
				bitmap.compress(CompressFormat.JPEG, quality, bos);
				sendImage(bos.toByteArray());
			} catch (OutOfMemoryError e) {
				Toast.makeText(getApplicationContext()
						, "Out of memory,  please decrease image quality"
						, Toast.LENGTH_SHORT).show();
				e.printStackTrace();
				finish();
			}
		}
	}

	public void decodeYUV420(int[] rgb, byte[] yuv420, int width, int height) {
		final int frameSize = width * height;

		for (int j = 0, yp = 0; j < height; j++) {
			int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
			for (int i = 0; i < width; i++, yp++) {
				int y = (0xff & ((int) yuv420[yp])) - 16;
				if (y < 0) y = 0;
				if ((i & 1) == 0) {
					v = (0xff & yuv420[uvp++]) - 128;
					u = (0xff & yuv420[uvp++]) - 128;
				}

				int y1192 = 1192 * y;
				int r = (y1192 + 1634 * v);
				int g = (y1192 - 833 * v - 400 * u);
				int b = (y1192 + 2066 * u);

				if (r < 0) r = 0; else if (r > 262143) r = 262143;
				if (g < 0) g = 0; else if (g > 262143) g = 262143;
				if (b < 0) b = 0; else if (b > 262143) b = 262143;

				rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
			}
		}
	}

	public void sendImage(byte[] data) {
		try {
			dos.writeInt(data.length);
			dos.write(data);
			out.flush();
		} catch (IOException e) {
			Log.e(TAG_IOIO, e.toString());
			connect_state = false;
		} catch (NullPointerException e) {
			Log.e(TAG_IOIO, e.toString());
		}
	}

	public void sendString(String str) {
		try {
			dos.writeInt(str.length());
			dos.write(str.getBytes());
			out.flush();
		} catch (IOException e) {
			Log.e(TAG_IOIO, e.toString());
			connect_state = false;
		} catch (NullPointerException e) {
			Log.e(TAG_IOIO, e.toString());
		}
	}

	/*public void sendZg(String str) {
		try {
			//dos.writeInt(str.length());
			//dos.write(str.getBytes());
			zg.write(str);
			out.flush();
			zg.close();
		} catch (IOException e) {
			Log.e(TAG_IOIO, e.toString());
			connect_state = false;
		} catch (NullPointerException e) {
			Log.e(TAG_IOIO, e.toString());
		}
	}*/

	public String getIP() {
		WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		Method[] wmMethods = wifi.getClass().getDeclaredMethods();
		for(Method method: wmMethods){
			if(method.getName().equals("isWifiApEnabled")) {

				try {
					if(method.invoke(wifi).toString().equals("false")) {
						WifiInfo wifiInfo = wifi.getConnectionInfo();
						int ipAddress = wifiInfo.getIpAddress();
						String ip = (ipAddress & 0xFF) + "." +
								((ipAddress >> 8 ) & 0xFF) + "." +
								((ipAddress >> 16 ) & 0xFF) + "." +
								((ipAddress >> 24 ) & 0xFF ) ;
						return ip;
					} else if(method.invoke(wifi).toString().equals("true")) {
						return "192.168.43.1";
					}
				} catch (IllegalArgumentException e) {
				} catch (IllegalAccessException e) {
				} catch (InvocationTargetException e) {
				}
			}
		}
		return "Unknown";
	}

	public Camera.Size getMaxPictureSize(Camera.Parameters params) {
		List<Camera.Size> pictureSize = params.getSupportedPictureSizes();
		int firstPictureWidth, lastPictureWidth;
		try {
			firstPictureWidth = pictureSize.get(0).width;
			lastPictureWidth = pictureSize.get(pictureSize.size() - 1).width;
			if(firstPictureWidth > lastPictureWidth)
				return pictureSize.get(0);
			else
				return pictureSize.get(pictureSize.size() - 1);
		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
			return pictureSize.get(0);
		}
	}

	public int getMaxPreviewFps(Camera.Parameters params) {
		List<Integer> previewFps = params.getSupportedPreviewFrameRates();
		int fps = 0;
		for(int i = 0 ; i < previewFps.size() ; i++) {
			if(previewFps.get(i) > fps)
				fps = previewFps.get(i);
		}
		return fps;
	}

	/****************************************************** functions from IOIOActivity *********************************************************************************/

	/*class Looper extends BaseIOIOLooper
	{

		PwmOutput speed, steering, pan, tilt;
		DigitalOutput led_;
//		int pwm_left_motor, pwm_right_motor;


		protected void setup() throws ConnectionLostException
		{
			//ioio.lib.api.IOIO ioio_ = new IOIO();
			pwm_speed = DEFAULT_PWM;
			pwm_steering = DEFAULT_PWM;
			pwm_pan = DEFAULT_PWM;
			pwm_tilt = DEFAULT_PWM;

			speed = ioio_.openPwmOutput(3, 50);
			steering = ioio_.openPwmOutput(4, 50);
			pan = ioio_.openPwmOutput(5, 50);
			tilt = ioio_.openPwmOutput(6, 50);

			speed.setPulseWidth(pwm_speed);
			steering.setPulseWidth(pwm_steering);
			pan.setPulseWidth(pwm_pan);
			tilt.setPulseWidth(pwm_tilt);

			led_ = ioio_.openDigitalOutput(IOIO.LED_PIN, true);

			Log.e("11","miao111111111111111111111");
			runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(getApplicationContext(),
							"Connected!", Toast.LENGTH_SHORT).show();
				}
			});
		}

		public void loop() throws ConnectionLostException, InterruptedException
		{
			led_.write(true);
			if(pwm_speed > MAX_PWM) pwm_speed = MAX_PWM;
			else if(pwm_speed < MIN_PWM) pwm_speed = MIN_PWM;

			if(pwm_steering > MAX_PWM) pwm_steering = MAX_PWM;
			else if(pwm_steering < MIN_PWM) pwm_steering = MIN_PWM;

			if(pwm_pan > MAX_PWM) pwm_pan = MAX_PWM;
			else if(pwm_pan < MIN_PWM) pwm_pan = MIN_PWM;

			if(pwm_tilt > MAX_PWM) pwm_tilt = MAX_PWM;
			else if(pwm_tilt < MIN_PWM) pwm_tilt = MIN_PWM;

        	Log.e("IOIO", "pwm_left_motor: " + pwm_speed + " pwm_right_motor: " + pwm_steering+ " pwm_pan: " + pwm_pan+ " pwm_tilt: " + pwm_tilt);

			speed.setPulseWidth(pwm_speed);
			steering.setPulseWidth(pwm_steering);
			pan.setPulseWidth(pwm_pan);
			tilt.setPulseWidth(pwm_tilt);

			Thread.sleep(20);
		}

		public void disconnected() {
			runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(getApplicationContext(),
							"Disonnected!", Toast.LENGTH_SHORT).show();
				}
			});
		}

		public void incompatible() {
			runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(getApplicationContext(),
							"Imcompatible firmware version", Toast.LENGTH_SHORT).show();
				}
			});
		}
	}

	protected IOIOLooper createIOIOLooper() {

		return new Looper();
	}

	@Override
	public IOIOLooper createIOIOLooper(String connectionType, Object extra) {
		return createIOIOLooper();
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		if ((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
		{
			helper_.restart();
		}
	}*/

	protected IOIOLooper createIOIOLooper()
	{
		return new IOIO_thread(this);				//  !!!!!!!!!!!!!!!!!   create our own IOIO thread (Looper) with a reference to this activity
	}

	@Override
	public IOIOLooper createIOIOLooper(String connectionType, Object extra)
	{
		return createIOIOLooper();
	}

	@Override
	protected void onDestroy()
	{
		helper_.destroy();
		super.onDestroy();
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		helper_.start();
	}

	@Override
	protected void onStop()
	{
		helper_.stop();
		super.onStop();
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		if ((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
		{
			helper_.restart();
		}
	}

	/*private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
				case LoaderCallbackInterface.SUCCESS:
				{
					Log.i(TAG, "OpenCV loaded successfully");
					//mOpenCvCameraView.enableView();
					//mOpenCvCameraView.setOnTouchListener(Main_activity.this);
				} break;
				default:
				{
					super.onManagerConnected(status);
				} break;
			}
		}
	};

	@Override
	public void onResume() {
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
		super.onResume();

	}



	public void onCameraViewStarted(int width, int height) {
		Log.e(TAG,"22222222222222");//摄像头启动方法
		mRgba = new Mat(height, width, CvType.CV_8UC4);
		//定义一幅图像，设置为4通道8位无符号型
		mDetector = new ColorBlobDetector();
		mSpectrum = new Mat();
		mBlobColorRgba = new Scalar(255);
		//定义一个单色像素
		mBlobColorHsv = new Scalar(255);
		SPECTRUM_SIZE = new Size(200, 64);
		CONTOUR_COLOR = new Scalar(255, 255, 0, 255);Log.e(TAG,"Successfuly1");

		//mDetector.setHsvColor(new Scalar(7, 196, 144)); //construction paper red
		//mDetector.setHsvColor(new Scalar(7.015625,255.0,239.3125)); //bucket orange
		//mDetector.setHsvColor(new Scalar(1.0,244.828125,205)); //box red
		mDetector.setHsvColor(new Scalar(8.0,195.21875,222.140625)); // ball orange
		//mDetector.setHsvColor(new Scalar(252.46875,219.15625,90.6875)); // cup red
		//mDetector.setHsvColor(new Scalar(108.0625,246.59375,170.734375)); //sparx green
		//mDetectorGreen.setHsvColor(new Scalar(58.09375,218.9375,107.75)); //medium aldrich green
	}

	public void onCameraViewStopped() {
		mRgba.release();
	}
	//释放摄像头，相机是一个共享资源，所以应该被谨慎管理，这样应用之间才不会发生冲突
	//所以使用完相机之后应该调用 Camera.release()来释放相机对象。
	//如果不释放，后续的使用相机请求（其他应用或本应用）都会失败。

	*//*public boolean onTouch(View v, MotionEvent event) {
		int cols = mRgba.cols();
		int rows = mRgba.rows();

		int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
		int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

		int x = (int)event.getX() - xOffset;
		int y = (int)event.getY() - yOffset;

		Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

		if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

		Rect touchedRect = new Rect();

		touchedRect.x = (x>4) ? x-4 : 0;
		touchedRect.y = (y>4) ? y-4 : 0;

		touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
		touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

		Mat touchedRegionRgba = mRgba.submat(touchedRect);

		Mat touchedRegionHsv = new Mat();
		Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

		// Calculate average color of touched region
		mBlobColorHsv = Core.sumElems(touchedRegionHsv);
		int pointCount = touchedRect.width*touchedRect.height;
		for (int i = 0; i < mBlobColorHsv.val.length; i++)
			mBlobColorHsv.val[i] /= pointCount;

		mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

		Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
				", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

		mDetector.setHsvColor(mBlobColorHsv);

		Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

		mIsColorSelected = true;

		touchedRegionRgba.release();
		touchedRegionHsv.release();

		return false; // don't need subsequent touch events
	}*//*

	private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
		Mat pointMatRgba = new Mat();
		Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
		Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

		return new Scalar(pointMatRgba.get(0, 0));
	}

	public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
		Log.e(TAG,"1111111111111111111111");
		mRgba = inputFrame.rgba();

		//setText("sonar1: "+m_ioio_thread.get_sonar1_reading(), sonar1Text);
		//setText("sonar2: "+m_ioio_thread.get_sonar2_reading(), sonar2Text);
		//setText("sonar3: "+m_ioio_thread.get_sonar3_reading(), sonar3Text);

		//if (mIsColorSelected) {
			mDetector.process(mRgba);
			List<MatOfPoint> contours = mDetector.getContours();
			Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

			Mat colorLabel = mRgba.submat(4, 68, 4, 68);
			colorLabel.setTo(mBlobColorRgba);

			Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70,
					70 + mSpectrum.cols());
			mSpectrum.copyTo(spectrumLabel);
		//}


		*//*if (autoMode) { // only move if autoMode is on
			//adjust pan/tilt
			double momentX = mDetector.getMomentX();
			double momentY = mDetector.getMomentY();

			if (mDetector.blobsDetected() == 0) {
				if (panningRight) {
					m_ioio_thread.pan(panVal += 30);
					Log.i(TAG, "左");
					if (panVal >= 2200)
						panningRight = false;
					Log.i(TAG, "0");
				} else {
					m_ioio_thread.pan(panVal -= 30);
					Log.i(TAG, "右");
					if (panVal <= 600)
						panningRight = true;
				}
				*//**//*
				if(tiltingUp){
					m_ioio_thread.tilt(tiltVal+=30);
					if(tiltVal >= 2000)
						tiltingUp = false;
				} else {
					m_ioio_thread.tilt(tiltVal-=30);
					if(tiltVal <= 1000)
						tiltingUp = true;
				}
				*//**//*
			} else {
				panInc = 40 + (int) Math.exp(.03 * Math.abs(momentX));

				if (momentX > 25) {
					m_ioio_thread.pan(panVal -= panInc);
					Log.i(TAG, "momentR");
				} else if (momentX < -25) {
					m_ioio_thread.pan(panVal += panInc);
					Log.i(TAG, "momentL");
				}
				tiltInc = 20 + (int) Math.exp(.03 * Math.abs(momentY));
				if (momentY > 25) {
					m_ioio_thread.tilt(tiltVal += tiltInc);
					Log.i(TAG, "momentDOWN");
				} else if (momentY < -25) {
					m_ioio_thread.tilt(tiltVal -= tiltInc);
					Log.i(TAG, "momentUP");
				}
			}

			if (panVal > 2200) panVal = 2200;
			if (panVal < 600) panVal = 600;
			if (tiltVal > 2000) tiltVal = 2000;
			if (tiltVal < 1000) tiltVal = 1000;

			//move
			//handle obstacles
			int sonar1 = m_ioio_thread.get_sonar1_reading();
			int sonar2 = m_ioio_thread.get_sonar2_reading();
			int sonar3 = m_ioio_thread.get_sonar3_reading();

			//if (sonar2 < 30){
                *//**//*else{
                    avoidingObstacle = true;
                }
            }
            else if(avoidingObstacle){
                if(sonar2 < 30){
                    if(sonar1 > sonar3){
                        m_ioio_thread.turn(1500-80);
                    } else {
                        m_ioio_thread.turn(1500+80);
                    }
                } else {
                    m_ioio_thread.move(1500+forward_speed);
                }
                if(sonar1 >= 20 && sonar2 >= 20 && sonar3 >= 20){
                    avoidingObstacle = false;
                }
*//**//*
			if (*//**//*sonar2 < 20 &&*//**//* mDetector.getMaxArea() > .05 * 4 * mDetector.getCenterX() * mDetector.getCenterY()) {
				m_ioio_thread.turn(1500);
				m_ioio_thread.move(1500);
				Log.i(TAG, "back");
			}
			//}
			//follow blob
			else {
				if (mDetector.blobsDetected() > 0) {


					if (!(panVal < 1650 && panVal > 1350)) {
						m_ioio_thread.move(1500 + forward_speed);
						if (panVal > 1500) {
							m_ioio_thread.turn(1500 + turn_speed);
							Log.i(TAG, "left");
						} else {
							m_ioio_thread.turn(1500 - turn_speed);
							Log.i(TAG, "right");
						}
					} else {
						m_ioio_thread.turn(1500);
						m_ioio_thread.move(1500 + forward_speed);
					}
				} else {
					m_ioio_thread.turn(1500);
					m_ioio_thread.move(1500);
				}*//*
			//}

		//}


		return mRgba;
	}*/
}