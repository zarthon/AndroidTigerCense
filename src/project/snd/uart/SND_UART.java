package project.snd.uart;

import project.snd.uart.R;
import project.snd.uart.SND_UART.IOIOThread;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.AbstractIOIOActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.ToggleButton;

public class SND_UART extends AbstractIOIOActivity {
	private ToggleButton button_;
	private TextView mesg_;
	private Uart uart; 
	private InputStream in;
	private OutputStream out;
	Boolean mbusy = false;
	/**
	 * Called when the activity is first created. Here we normally initialize
	 * our GUI.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		button_ = (ToggleButton) findViewById(R.id.button);
		mesg_ = (TextView) findViewById(R.id.title);
	}

	/**
	 * This is the thread on which all the IOIO activity happens. It will be run
	 * every time the application is resumed and aborted when it is paused. The
	 * method setup() will be called right after a connection with the IOIO has
	 * been established (which might happen several times!). Then, loop() will
	 * be called repetitively until the IOIO gets disconnected.
	 */
	class IOIOThread extends AbstractIOIOActivity.IOIOThread {
		/** The on-board LED. */
		private DigitalOutput led_;
		private boolean command_flag = false;

		/**
		 * Called every time a connection with IOIO has been established.
		 * Typically used to open pins.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#setup()
		 */
		@Override
		protected void setup() throws ConnectionLostException {
			led_ = ioio_.openDigitalOutput(0, true);
			//iWrite("test");
			try{
				uart = ioio_.openUart(4,5, 57600,Uart.Parity.NONE,Uart.StopBits.ONE); 
				in = uart.getInputStream(); 
				out = uart.getOutputStream();
			}
			catch(ConnectionLostException e){
				iWrite(e.getMessage()); 
				iWrite(Log.getStackTraceString(e)); 
			}
		}

		/**
		 * Called repetitively while the IOIO is connected.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#loop()
		 */
		@Override
		protected void loop() throws ConnectionLostException {
			led_.write(!button_.isChecked());
			try {
				if(!mbusy){
					mbusy = true;
					String command1 = "$CMD1";
					String command2  = "$CMD2";
					if(command_flag){
						out.write(command2.getBytes());
					}
					else{
						out.write(command1.getBytes());
					}
					sleep(100);
					testUart();
					//iWrite("test");
				}
			} catch (InterruptedException e) {
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		private void testUart(){
			try{
				int available = in.available();
				if(available > 0 ){
					byte[] readBuffer = new byte[70];
					in.read(readBuffer,0,available);
					char[] temp = (new String(readBuffer,0,available)).toCharArray();
					String temp2 = new String(temp);
					//Post retrieval data processing
					iWrite(temp2 + '\n');
					if(temp2.contains("ACK1")){
						command_flag = true;
					}
					if(command_flag){
						iWrite(temp2 + '\n');
					}
				}
				sleep(500);

			}
			catch(InterruptedException e){
				iWrite("Eror:" + e);

			}
			catch(IOException e){
				iWrite("Error"+e);

			}
			mbusy=false;
		}

		private void iWrite(String myval){

			final String crossValue = myval;
			runOnUiThread(new Runnable() {
				@Override
				public void run(){
					mesg_.setText(mesg_.getText() + crossValue);
				}
			});
		}
	}

	/**
	 * A method to create our IOIO thread.
	 * 
	 * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
	 */
	@Override
	protected AbstractIOIOActivity.IOIOThread createIOIOThread() {
		return new IOIOThread();
	}
}