
package com.fyp.bikev5;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.UUID;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.fyp.bikev5.MapsActivity;

import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;

import static android.support.v4.content.ContextCompat.getSystemService;

public class ChatService {
	private static final String NAME_SECURE = "BluetoothChatSecure";
	private static final String NAME_INSECURE = "BluetoothChatInsecure";

	// Unique UUID for this application
	private static final UUID MY_UUID_SECURE = UUID
			.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
	private static final UUID MY_UUID_INSECURE = UUID
			.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

	// Member fields
	private final BluetoothAdapter bluetoothAdapter;
	private final Handler handler;
	private AcceptThread secureAcceptThread;
	private AcceptThread insecureAcceptThread;
	private ConnectThread connectThread;
	private ConnectedThread connectedThread;
	private int state;

	// Constants that indicate the current connection state
	public static final int STATE_NONE = 0;
	public static final int STATE_LISTEN = 1; // listening connection
	public static final int STATE_CONNECTING = 2; // initiate outgoing
	// connection
	public static final int STATE_CONNECTED = 3; // connected to remote device

	OutputStream os;
	BufferedWriter writer;
	URL url;
	JSONObject postDataParams;
	HttpURLConnection conn;
	PrefManager pref;
	Context cnt;



	public ChatService(Context context, Handler handler) {
		cnt=context;
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		state = STATE_NONE;
		pref=new PrefManager(context);
		String temp = "";
		OutputStream os = null;
		BufferedWriter writer = null;
		this.handler = handler;


	}

	// Set the current state of the chat connection
	private synchronized void setState(int state) {
		this.state = state;

		handler.obtainMessage(MapsActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
	}

	// get current connection state
	public synchronized int getState() {
		return state;
	}

	// start service
	public synchronized void start() {
		// Cancel any thread
		if (connectThread != null) {
			connectThread.cancel();
			connectThread = null;
		}

		// Cancel any running thresd
		if (connectedThread != null) {
			connectedThread.cancel();
			connectedThread = null;
		}

		setState(STATE_LISTEN);

		// Start the thread to listen on a BluetoothServerSocket
		if (secureAcceptThread == null) {
			secureAcceptThread = new AcceptThread(true);
			secureAcceptThread.start();
		}
		if (insecureAcceptThread == null) {
			insecureAcceptThread = new AcceptThread(false);
			insecureAcceptThread.start();
		}
	}



	// initiate connection to remote device
	public synchronized void connect(BluetoothDevice device, boolean secure) {
		// Cancel any thread
		if (state == STATE_CONNECTING) {
			if (connectThread != null) {
				connectThread.cancel();
				connectThread = null;
			}
		}

		// Cancel running thread
		if (connectedThread != null) {
			connectedThread.cancel();
			connectedThread = null;
		}

		// Start the thread to connect with the given device
		connectThread = new ConnectThread(device, secure);
		connectThread.start();
		setState(STATE_CONNECTING);
	}

	// manage Bluetooth connection
	public synchronized void connected(BluetoothSocket socket,
									   BluetoothDevice device, final String socketType) {
		// Cancel the thread
		if (connectThread != null) {
			connectThread.cancel();
			connectThread = null;
		}

		// Cancel running thread
		if (connectedThread != null) {
			connectedThread.cancel();
			connectedThread = null;
		}

		if (secureAcceptThread != null) {
			secureAcceptThread.cancel();
			secureAcceptThread = null;
		}
		if (insecureAcceptThread != null) {
			insecureAcceptThread.cancel();
			insecureAcceptThread = null;
		}

		// Start the thread to manage the connection and perform transmissions
		connectedThread = new ConnectedThread(socket, socketType);
		connectedThread.start();

		// Send the name of the connected device back to the UI Activity
		Message msg = handler.obtainMessage(MapsActivity.MESSAGE_DEVICE_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(MapsActivity.DEVICE_NAME, device.getName());
		msg.setData(bundle);
		handler.sendMessage(msg);

		setState(STATE_CONNECTED);
	}

	// stop all threads
	public synchronized void stop() {
		if (connectThread != null) {
			connectThread.cancel();
			connectThread = null;
		}

		if (connectedThread != null) {
			connectedThread.cancel();
			connectedThread = null;
		}

		if (secureAcceptThread != null) {
			secureAcceptThread.cancel();
			secureAcceptThread = null;
		}

		if (insecureAcceptThread != null) {
			insecureAcceptThread.cancel();
			insecureAcceptThread = null;
		}
		setState(STATE_NONE);
	}

	public void write(byte[] out) {
		ConnectedThread r;
		synchronized (this) {
			if (state != STATE_CONNECTED)
				return;
			r = connectedThread;
		}
		r.write(out);
	}

	private void connectionFailed() {
		Message msg = handler.obtainMessage(MapsActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MapsActivity.TOAST, "Unable to connect device");
		msg.setData(bundle);
		handler.sendMessage(msg);

		// Start the service over to restart listening mode
		ChatService.this.start();
	}

	private void connectionLost() {
		Message msg = handler.obtainMessage(MapsActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MapsActivity.TOAST, "Device connection was lost");
		msg.setData(bundle);
		handler.sendMessage(msg);

		// Start the service over to restart listening mode
		ChatService.this.start();
	}

	// runs while listening for incoming connections
	private class AcceptThread extends Thread {
		private final BluetoothServerSocket serverSocket;
		private String socketType;

		public AcceptThread(boolean secure) {
			BluetoothServerSocket tmp = null;
			socketType = secure ? "Secure" : "Insecure";

			try {
				if (secure) {
					tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
							NAME_SECURE, MY_UUID_SECURE);
				} else {
					tmp = bluetoothAdapter
							.listenUsingInsecureRfcommWithServiceRecord(
									NAME_INSECURE, MY_UUID_INSECURE);
				}
			} catch (IOException e) {
			}
			serverSocket = tmp;
		}

		public void run() {
			setName("AcceptThread" + socketType);

			BluetoothSocket socket = null;

			while (state != STATE_CONNECTED) {
				try {
					socket = serverSocket.accept();
				} catch (IOException e) {
					break;
				}

				// If a connection was accepted
				if (socket != null) {
					synchronized (ChatService.this) {
						switch (state) {
							case STATE_LISTEN:
							case STATE_CONNECTING:
								// start the connected thread.
								connected(socket, socket.getRemoteDevice(),
										socketType);
								break;
							case STATE_NONE:
							case STATE_CONNECTED:
								// Either not ready or already connected. Terminate
								// new socket.
								try {
									socket.close();
								} catch (IOException e) {
								}
								break;
						}
					}
				}
			}
		}

		public void cancel() {
			try {
				serverSocket.close();
			} catch (IOException e) {
			}
		}
	}

	// runs while attempting to make an outgoing connection
	private class ConnectThread extends Thread {
		private final BluetoothSocket socket;
		private final BluetoothDevice device;
		private String socketType;

		public ConnectThread(BluetoothDevice device, boolean secure) {
			this.device = device;
			BluetoothSocket tmp = null;
			socketType = secure ? "Secure" : "Insecure";

			try {
				if (secure) {
					tmp = device
							.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
				} else {
					tmp = device
							.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
				}
			} catch (IOException e) {
			}
			socket = tmp;
		}

		public void run() {
			setName("ConnectThread" + socketType);

			// Always cancel discovery because it will slow down a connection
			bluetoothAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			try {
				socket.connect();
			} catch (IOException e) {
				try {
					socket.close();
				} catch (IOException e2) {
				}
				connectionFailed();
				return;
			}

			// Reset the ConnectThread because we're done
			synchronized (ChatService.this) {
				connectThread = null;
			}

			// Start the connected thread
			connected(socket, device, socketType);
		}

		public void cancel() {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}

	// runs during a connection with a remote device
	private class ConnectedThread extends Thread {
		private final BluetoothSocket bluetoothSocket;
		private final InputStream inputStream;
		private final OutputStream outputStream;

		public ConnectedThread(BluetoothSocket socket, String socketType) {
			this.bluetoothSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
			}

			inputStream = tmpIn;
			outputStream = tmpOut;
		}

		public void run() {
			byte[] buffer = new byte[1024];
			int bytes;




			// Keep listening to the InputStream
			while (true) {
				try {
					url = new URL(pref.getIpAddress());
					postDataParams = new JSONObject();
					conn = (HttpURLConnection) url.openConnection();
					conn.setReadTimeout(15000 /* milliseconds */);
					conn.setConnectTimeout(15000 /* milliseconds */);
					conn.setRequestMethod("POST");
					conn.setDoInput(true);
					conn.setDoOutput(true);

					os = conn.getOutputStream();
					writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

					// Read from the InputStream
					bytes = inputStream.read(buffer);
					// Send the obtained bytes to the UI Activity
					handler.obtainMessage(MapsActivity.MESSAGE_READ, bytes, -1,
							buffer).sendToTarget();
					Log.d("BLUETOOTH OUTPUT", String.valueOf(bytes));

					try {
						String readMessage = new String(buffer, 0, bytes);

						postDataParams.put("name", readMessage);
						Log.e("params", postDataParams.toString());

						writer.write(URLEncoder.encode(readMessage, "UTF-8"));
						writer.flush();

						int responseCode = conn.getResponseCode();
						if (responseCode == HttpsURLConnection.HTTP_OK) {

							BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
							StringBuffer sb = new StringBuffer("");
							String line = "";

							if((line = in.readLine()) != null) {
								buildNotification(line);
							}
							in.close();
							//return sb.toString();
							//Toast.makeText(getApplicationContext(), sb.toString(),Toast.LENGTH_LONG).show();
							//Log.e("********OUTPUT*******", sb.toString());
						} else {
							//return new String("false : " + responseCode);
							//Toast.makeText(context, new String("false : " + responseCode),Toast.LENGTH_LONG).show();
							Log.d("ERROR CHATASERVICE", String.valueOf(responseCode));
						}
					} catch (JSONException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (IOException e) {
					connectionLost();
					// Start the service over to restart listening mode
					ChatService.this.start();
					break;
				}
			}
		}


		private void buildNotification(String data) {

			//Get an instance of NotificationManager//

			NotificationCompat.Builder mBuilder =
					new NotificationCompat.Builder(cnt)
							.setSmallIcon(R.drawable.ic_launcher_foreground)
							.setContentTitle("NOTIFICATION")
							.setContentText("Probability that machine will fail within \n 30 days: " + data)
							.setStyle(new NotificationCompat.BigTextStyle()
									.bigText("Probability that machine will fail within \n 30 days: " + data));


			// Gets an instance of the NotificationManager service//

			NotificationManagerCompat notificationManager = NotificationManagerCompat.from(cnt);
			notificationManager.notify(123,mBuilder.build());

		}


		// write to OutputStream
		public void write(byte[] buffer) {
			try {
				outputStream.write(buffer);
				handler.obtainMessage(MapsActivity.MESSAGE_WRITE, -1, -1,
						buffer).sendToTarget();
			} catch (IOException e) {
			}
		}

		public void cancel() {
			try {
				bluetoothSocket.close();
			} catch (IOException e) {
			}
		}

		public String getPostDataString(JSONObject params) throws Exception {

			StringBuilder result = new StringBuilder();
			boolean first = true;

			Iterator<String> itr = params.keys();

			while (itr.hasNext()) {

				String key = itr.next();
				Object value = params.get(key);

				result.append(URLEncoder.encode(value.toString(), "UTF-8"));

/*
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(key, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(value.toString(), "UTF-8"));
*/
			}
			return result.toString();
		}

	}
}
