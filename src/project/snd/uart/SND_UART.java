package project.snd.uart;

import project.snd.uart.R;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.AbstractIOIOActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class SND_UART extends AbstractIOIOActivity {
	private ToggleButton button_;
	private TextView mesg_;
	private Uart uart; 
	private ImageView image_;
	private InputStream in;
	private OutputStream out;
	Boolean mbusy = false;
	
	private static final byte[] HEXBYTES = { (byte) '0', (byte) '1', (byte) '2', (byte) '3',
	      (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) 'a',
	      (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f' };
	
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
		image_ = (ImageView) findViewById(R.id.receiverImage);
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
		private boolean file_flag = false;
		File dir,image;
		FileOutputStream fOut;
		String hexstream = new String();
		/**
		 * 
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
			
			try{
				/*
				 * Creating uart module with 4 as Rx pin and 5 as Tx pin
				 */
				
				uart = ioio_.openUart(4, 5, 115200, Uart.Parity.NONE, Uart.StopBits.ONE); 
				in = uart.getInputStream(); 
				out = uart.getOutputStream();
				/* 
				 * Check whether sd is mounted or not so as to save the file. Currently the file name is hard coded.
				 * 
				 */
				File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
				dir = new File (root.getAbsolutePath() + "/100MEDIA/");
				if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM))){
					image = new File(dir,"sample123.jpg");
					fOut = 	new FileOutputStream( image );
				}
			}
			catch(ConnectionLostException e){
				iWrite(e.getMessage(),false); 
				iWrite(Log.getStackTraceString(e),false); 
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
			if(!mbusy){
				mbusy = true;
				try {
					/*
					 * Either call simpleCommunication() or fileTransfer() for the function which needs to be demonstrated
					 */
					simpleCommunication();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
		private void simpleCommunication() throws IOException, InterruptedException{
			String command1 = "$CMD1";
			String command2  = "$CMD2";
			
			if(command_flag){
				out.write(command2.getBytes());
			}
			else{
				out.write(command1.getBytes());
			}
			sleep(10);
			
			try{
				/*
				 * Check whether any data available in the buffer or not. If there is than collect it and do the processing.
				 */
				int available = in.available();
				if(available > 0 ){
					byte[] readBuffer = new byte[200];
					in.read(readBuffer,0,available);

					char[] temp = (new String(readBuffer,0,available)).toCharArray();
					String temp2 = new String(temp);
					
					//Post retrieval data processing
					//iWrite(temp2 + '\n',false);
					
					if(temp2.contains("ACK1") && !command_flag){
						iWrite(temp2 + '\n', false);
						command_flag = true;
					}
					if(command_flag){
						iWrite(temp2 + '\n', false);
					}
				}
				else{
				}
				sleep(10);
			}
			catch(InterruptedException e){
				iWrite("Eror:" + e,false);
			}
			catch(IOException e){
				iWrite("Error"+e,false);

			}
			mbusy=false;
		}
		
		private void fileTransfer(){
			try{
				int available = in.available();
				if(available > 0 ){
					byte[] readBuffer = new byte[200];
					in.read(readBuffer,0,available);
					fOut.write(readBuffer,0,available);
					int len = available;
				    char[] s = new char[available * 2];
			    	/*
			    	 * Convert the byte stream into hex string and check for the end bytes of the image
			    	 */
				    for (int i = 0, j = 0; i < len; i++) {
				      int c = ((int) readBuffer[i]) & 0xff;

				      s[j++] = (char) HEXBYTES[c >> 4 & 0xf];
				      s[j++] = (char) HEXBYTES[c & 0xf];
				    }
				    hexstream = hexstream + new String(s);
				}
				else{
					if(hexstream.contains("ffd9") && !file_flag){
						iWrite("File closed",false);
						file_flag = true;
						fOut.close();
						/*
						 * Trigger the MediaScanner Connection so that the image is immediately available
						 * in the gallery and now on completed can be loaded into the application
						 */
						MediaScannerConnection.scanFile(getBaseContext(),
						          new String[] { image.toString() }, null,
						          new MediaScannerConnection.OnScanCompletedListener() {	
									@Override
									public void onScanCompleted(String path, Uri uri) {
										/*
										 * Update the image in the application to the newely retrieved image.
										 */
										iWrite(path,true);
										
									}
								});
					}
					//iWrite("No character");
				}
				sleep(10);

			}
			catch(InterruptedException e){
				iWrite("Eror:" + e,false);

			}
			catch(IOException e){
				iWrite("Error"+e,false);

			}
			mbusy=false;
		}
		/*
		 * TO change the values of textview and imageview at run time.
		 */
		private void iWrite(String myval, boolean image){

			final String crossValue = myval;
			final boolean imageV = image;
			runOnUiThread(new Runnable() {
				@Override
				public void run(){
					if(imageV){
						/*
						 * Currently the path is hard coded. This can be dynamically set. 
						 */
						Bitmap bMap = BitmapFactory.decodeFile("/sdcard/DCIM/100MEDIA/sample123.jpg");
					    image_.setImageBitmap(bMap);
					}
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