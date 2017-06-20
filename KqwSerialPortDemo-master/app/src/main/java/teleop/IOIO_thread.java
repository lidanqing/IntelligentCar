package teleop;

/**
 * Created by li on 2017/5/25.
 */

import android.util.Log;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;

public class IOIO_thread extends BaseIOIOLooper
{
    private AnalogInput input_;
    private PwmOutput pwmOutput_;
    public static DigitalOutput led;
    IOIOu the_gui;					// reference to the main activity
    PwmOutput speed, steering, pan, tilt;

    public IOIO_thread(IOIOu gui)
    {
        the_gui = gui;
    }

    @Override
    public void setup() throws ConnectionLostException
    {
        try {
            input_ = ioio_.openAnalogInput(40);
            pwmOutput_ = ioio_.openPwmOutput(7, 100);
            led = ioio_.openDigitalOutput(9, false);

            //the_gui.enableUi(true);
            IOIOu.pwm_speed = IOIOu.DEFAULT_PWM;
            IOIOu.pwm_steering = IOIOu.DEFAULT_PWM;
            IOIOu.pwm_pan = IOIOu.DEFAULT_PWM;
            IOIOu.pwm_tilt = IOIOu.DEFAULT_PWM;

            speed = ioio_.openPwmOutput(3, 50);
            steering = ioio_.openPwmOutput(4, 50);
            pan = ioio_.openPwmOutput(5, 50);
            tilt = ioio_.openPwmOutput(6, 50);

            speed.setPulseWidth(IOIOu.pwm_speed);
            steering.setPulseWidth(IOIOu.pwm_steering);
            pan.setPulseWidth(IOIOu.pwm_pan);
            tilt.setPulseWidth(IOIOu.pwm_tilt);

            Log.e("11","miao111111111111111111111");
        } catch (ConnectionLostException e)
        {
            //the_gui.enableUi(false);
            throw e;
        }
    }

    @Override
    public void loop() throws ConnectionLostException
    {
        try
        {
            final float reading = input_.read();
            //the_gui.setText(Float.toString(reading));

           // pwmOutput_.setPulseWidth(500 + the_gui.seekBar_.getProgress() * 2);
            //led_.write(!the_gui.toggleButton_.isChecked());
            if(IOIOu.pwm_speed > IOIOu.MAX_PWM) IOIOu.pwm_speed = IOIOu.MAX_PWM;
            else if(IOIOu.pwm_speed < IOIOu.MIN_PWM) IOIOu.pwm_speed = IOIOu.MIN_PWM;

            if(IOIOu.pwm_steering > IOIOu.MAX_PWM) IOIOu.pwm_steering = IOIOu.MAX_PWM;
            else if(IOIOu.pwm_steering < IOIOu.MIN_PWM) IOIOu.pwm_steering = IOIOu.MIN_PWM;

            if(IOIOu.pwm_pan > IOIOu.MAX_PWM) IOIOu.pwm_pan = IOIOu.MAX_PWM;
            else if(IOIOu.pwm_pan <IOIOu. MIN_PWM) IOIOu.pwm_pan =IOIOu. MIN_PWM;

            if(IOIOu.pwm_tilt > IOIOu.MAX_PWM) IOIOu.pwm_tilt = IOIOu.MAX_PWM;
            else if(IOIOu.pwm_tilt < IOIOu.MIN_PWM) IOIOu.pwm_tilt = IOIOu.MIN_PWM;

            Log.e("IOIO", "pwm_left_motor: " + IOIOu.pwm_speed + " pwm_right_motor: " + IOIOu.pwm_steering+ " pwm_pan: " + IOIOu.pwm_pan+ " pwm_tilt: " +IOIOu. pwm_tilt);

            speed.setPulseWidth(IOIOu.pwm_speed);
            steering.setPulseWidth(IOIOu.pwm_steering);
            pan.setPulseWidth(IOIOu.pwm_pan);
            tilt.setPulseWidth(IOIOu.pwm_tilt);

            Thread.sleep(20);

            Thread.sleep(10);
        } catch (InterruptedException e) {
            ioio_.disconnect();
        } catch (ConnectionLostException e) {
            //the_gui.enableUi(false);
            throw e;
        }
    }
}
