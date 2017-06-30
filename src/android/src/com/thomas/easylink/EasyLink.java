package com.thomas.easylink;

import android.content.Context;
import android.net.wifi.WifiManager;
import com.mxchip.easylink_api.EasyLinkAPI;
import com.mxchip.ftc_service.FTC_Listener;
import com.mxchip.ftc_service.FTC_Service;
import com.mxchip.helper.EasyLinkWifiManager;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.*;


/**
 * This class launches the camera view, allows the user to take a picture,
 * closes the camera view, and returns the captured image. When the camera view
 * is closed, the screen displayed before the camera view was shown is
 * redisplayed.
 */
public class EasyLink extends CordovaPlugin {
	private EasyLinkWifiManager mWifiManager = null;
	private FTC_Service ftcService = null;
	private Socket socket = null;

	public EasyLinkAPI elapi;
//	private UDPServer server;
//	private Context ctx = null;

//	private CallbackContext callbackContext;
	private WifiManager.MulticastLock lock;
	@Override
	protected void pluginInitialize() {
//		super.pluginInitialize();
//		ctx = cordova.getActivity();

		WifiManager manager = (WifiManager) cordova.getActivity()
				.getSystemService(Context.WIFI_SERVICE);
		 lock= manager.createMulticastLock("test wifi");
	}
	@Override
	public boolean execute(String action, final JSONArray args,
			final CallbackContext callbackContext) throws JSONException {
		if ("beep".equals(action)) {
			callbackContext.success("micro sdk !!!");
			return true;
		} else if ("getWifiSSid".equals(action)) {
			mWifiManager = new EasyLinkWifiManager(webView.getContext());
			callbackContext.success(mWifiManager.getCurrentSSID());
			return true;
		} else if("configFTC".equals(action)) {
			OutputStream outputStream;
			try {
				outputStream = socket.getOutputStream();
				String toSend = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: "+args.getString(0).toString().length()+"\r\n\r\n"+args.getString(0).toString();
				outputStream.write(toSend.getBytes());
				ftcService.stopTransmitting();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				ftcService.stopTransmitting();
				e.printStackTrace();
			}
			callbackContext.success("true");

		} else if ("startSearch".equals(action)) {
			if(!checkTdmecParam(args,callbackContext)){
				return true;
			}
			String type = args.getString(2);
			if ("EasyLink_CT".equals(type)){
				ftcService(args, callbackContext);
			}else if("EasyLink".equals(type)){
//				this.callbackContext = callbackContext;
				easyLinkService(args,callbackContext);
			}
			return true;
		} else if ("stopSearch".equals(action)) {
//			if(ftcService != null) {
//				ftcService.stopTransmitting();
//			}
//			if (udpThread!=null&&udpThread.isAlive()){
////				udpThread.stop();
////				udpThread.interrupt();
//				life = false;
//				udpThread =null;
//			}
			stop();
			callbackContext.success("停止配网");
			return true;
		}
		return false; // Returning false results in a "MethodNotFound" error.
	}

	private void stop(){
		if(ftcService != null) {
			ftcService.stopTransmitting();
		}
		if (udpThread!=null&&udpThread.isAlive()){
//				udpThread.stop();
//				udpThread.interrupt();
			life = false;
			udpThread =null;
		}
	}

	@Override
	public void onDestroy() {
		stop();
		super.onDestroy();
	}

	private Thread udpThread;
	private static final int PORT = 20001;

	private byte[] msg = new byte[1024];

	/**
	 * TMDEC的easylink
	 * @param args
	 * @throws JSONException
	 */
	private void easyLinkService(JSONArray args,CallbackContext callbackContext)  {
		String ssid = null;
		String password = null;
		try {
			ssid = args.getString(0);
			password = args.getString(1);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if(elapi!=null) {
			elapi.stopEasyLink();
		}else {
			elapi = new EasyLinkAPI(cordova.getActivity());
		}
		if (udpThread!=null&&udpThread.isAlive()){
//			udpThread.stop();
//			udpThread.interrupt();
			lock.release();
			life = false;
			udpThread =null;
		}
		elapi.startEasyLink(cordova.getActivity(),ssid,password);
		udpThread = getUdpThread(callbackContext);
		lock.acquire();
		udpThread.start();

	}

	private boolean life = true;
	private Thread getUdpThread(final CallbackContext callbackContext){
		return new Thread(new Runnable() {
			@Override
			public void run() {
				DatagramSocket dSocket = null;
				DatagramPacket dPacket = new DatagramPacket(msg, msg.length);
				try {
					dSocket = new DatagramSocket(null);
					dSocket.setReuseAddress(true);
					dSocket.bind(new InetSocketAddress(PORT));
					life = true;
					while (life) {
						try {
							dSocket.receive(dPacket);
							int length = dPacket.getLength();
							byte[] receiveBytes = new byte[length];
							System.arraycopy(dPacket.getData(),dPacket.getOffset(),receiveBytes,0,length);
							String jsonStr;
							try {
								jsonStr = parseReceiveBytesToJsonStr(receiveBytes);
							} catch (JSONException e) {
								e.printStackTrace();
								jsonStr= "解析异常";
							}
							callbackContext.success(jsonStr);
						} catch (IOException e) {
							e.printStackTrace();
							callbackContext.error("接收udp数据出错");
						}
						life = false;
					}
//					dSocket.close();
				} catch (SocketException e) {
					e.printStackTrace();
				}
				if (dSocket!=null)
					dSocket.close();
				lock.release();
			}
		});
	}

	private String parseReceiveBytesToJsonStr(byte[] receiveBytes) throws JSONException {
		JSONObject jsonObject  = new JSONObject();
		byte[] psnBytes = new byte[8];
		System.arraycopy(receiveBytes,0,psnBytes,0,8);
		Long psn = byteToLong(psnBytes);
		String ip = new String(receiveBytes,8,(receiveBytes.length-8));
		jsonObject.put("psn",psn);
		jsonObject.put("ip",ip.trim());
		return jsonObject.toString();
	}

	//byte数组转成long
	private long byteToLong(byte[] b) {
		long s = 0;
		long s0 = b[0] & 0xff;// 最低位
		long s1 = b[1] & 0xff;
		long s2 = b[2] & 0xff;
		long s3 = b[3] & 0xff;
		long s4 = b[4] & 0xff;// 最低位
		long s5 = b[5] & 0xff;
		long s6 = b[6] & 0xff;
		long s7 = b[7] & 0xff;

		// s0不变
		s1 <<= 8;
		s2 <<= 16;
		s3 <<= 24;
		s4 <<= 8 * 4;
		s5 <<= 8 * 5;
		s6 <<= 8 * 6;
		s7 <<= 8 * 7;
		s = s0 | s1 | s2 | s3 | s4 | s5 | s6 | s7;
		return s;
	}

	private boolean checkTdmecParam(JSONArray args, CallbackContext callbackContext){
		try {
			String ssid = args.getString(0);
			String password = args.getString(1);
			if (isEmpty(ssid)){
				callbackContext.error("请输入wifi名称!");
				return false;
			}
			if (isEmpty(password)){
				callbackContext.error("请输入wifi密码!");
				return false;
			}
		} catch (JSONException e) {
			e.printStackTrace();
			callbackContext.error("参数错误!");
			return false;
		}
		return true;
	}

	/**
	 * SWITCH的easylink
	 * @param args
	 * @param callbackContext
     */
	private void ftcService(final JSONArray args, final CallbackContext callbackContext) {
		if(!checkSwitchParam(args,callbackContext)) {
			return;
		}
		cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    if(ftcService != null) {
                        ftcService.stopTransmitting();
                        ftcService = null;
                    }
                    ftcService = FTC_Service.getInstence();
					String ssid = args.getString(0);
					String password = args.getString(1);
					final String psn = args.getString(3);
					String serviceIp = args.getString(4);
					String port = args.getString(5);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("host",serviceIp);
                    jsonObject.put("port",Integer.valueOf(port));
                    ftcService.transmitSettings(ssid,password, jsonObject.toString(), mWifiManager.getCurrentIpAddressConnectedInt(),
                            new FTC_Listener(){

                        @Override
                        public void onFTCfinished(Socket s, String jsonString) {
                            // TODO Auto-generated method stub
                            socket = s;
							sendPsnToDevice(psn);
                            callbackContext.success(jsonString);

                        }

                        @Override
                        public void isSmallMTU(int MTU) {
                            // TODO Auto-generated method stub
                            System.out.println("isSmallMTU");
                        }

                    });
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    callbackContext.error("Wifi Init Error!");
                }
            }
        });
	}

	private boolean checkSwitchParam(JSONArray args, CallbackContext callbackContext){
		try {
//			String ssid = args.getString(0);
//			String password = args.getString(0);
//			String type = args.getString(0);
			String psn = args.getString(0);
			String serviceIp = args.getString(0);
			String port = args.getString(0);

//			if (isEmpty(ssid)){
//				callbackContext.error("请输入wifi名称!");
//				return false;
//			}
//			if (isEmpty(password)){
//				callbackContext.error("请输入wifi密码!");
//				return false;
//			}
//			if (isEmpty(type)){
//				callbackContext.error("请输入类型!");
//				return false;
//			}
			if (isEmpty(psn)){
				callbackContext.error("请输入psn!");
				return false;
			}
			if (isEmpty(serviceIp)){
				callbackContext.error("请输入服务器的ip!");
				return false;
			}
			if (isEmpty(port)){
				callbackContext.error("请输入服务器的端口!");
				return false;
			}
		} catch (JSONException e) {
			e.printStackTrace();
			callbackContext.error("参数错误!");
			return false;
		}
		return true;
	}

	private boolean isEmpty(String string){
		if (string!=null&&string.trim().length()!=0){
			return false;
		}
		return true;
	}

	private void sendPsnToDevice(String psn){
		OutputStream outputStream ;
		try {
			JSONObject accessKeyJson = new JSONObject();
			accessKeyJson.put("access_key",psn);
			String accessString = accessKeyJson.toString();
			outputStream = socket.getOutputStream();
			String toSend = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: "+accessString.length()+"\r\n\r\n"+accessString;
			outputStream.write(toSend.getBytes());
			ftcService.stopTransmitting();
			outputStream.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			ftcService.stopTransmitting();
			e.printStackTrace();
		}
	}
}