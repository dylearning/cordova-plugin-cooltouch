package com.thomas.easylink;

import android.util.Log;
import com.mxchip.ftc_service.FTC_Listener;
import com.mxchip.ftc_service.FTC_Service;
import com.mxchip.helper.EasyLinkWifiManager;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.Socket;

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

	@Override
	protected void pluginInitialize() {

	}
	@Override
	public boolean execute(String action, final JSONArray args,final CallbackContext callbackContext) throws JSONException {
		if ("startSearch".equals(action)) {

			Log.e("dengying","execute startSearch");

			mWifiManager = new EasyLinkWifiManager(webView.getContext());

			if(!checkTdmecParam(args,callbackContext)){
				return true;
			}

			ftcService(args, callbackContext);

			return true;
		} else if ("stopSearch".equals(action)) {
			Log.e("dengying","execute stopSearch");

			stop();
			callbackContext.success("停止配网");
			return true;
		}
		return false;
	}

	private void stop(){
		Log.e("dengying","execute stop");

		if(ftcService != null) {
			ftcService.stopTransmitting();
		}
	}

	@Override
	public void onDestroy() {
		stop();
		super.onDestroy();
	}

	private void ftcService(final JSONArray args, final CallbackContext callbackContext) {

		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				try {
					if(ftcService != null) {
						ftcService.stopTransmitting();
						ftcService = null;
					}
					ftcService = FTC_Service.getInstence();

					//String ssid = "HUAWEI-YS";
					//String password = "12345678";
					//String psn = "88740009";
					//String serviceIp = "101.231.241.28";
					//String port = "9775";

					String ssid = args.getString(0);
					String password = args.getString(1);
					final String psn = args.getString(2);
					String serviceIp = args.getString(3);
					String port = args.getString(4);
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("host",serviceIp);
					jsonObject.put("port",Integer.valueOf(port));

                    Log.e("dengying","ftcService.transmitSettings");

					ftcService.transmitSettings(ssid,password, "{\"host\":\""+serviceIp+"\",\"port\":"+port+"}", mWifiManager.getCurrentIpAddressConnectedInt(),
							new FTC_Listener(){
								@Override
								public void onFTCfinished(Socket s, String jsonString) {
									// TODO Auto-generated method stub
									Log.e("dengying","onFTCfinished");
									socket = s;
									sendPsnToDevice(psn);
									callbackContext.success(jsonString);
								}

								@Override
								public void isSmallMTU(int MTU) {
									// TODO Auto-generated method stub
									Log.e("dengying","isSmallMTU");
									System.out.println("isSmallMTU");
								}
							});
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					Log.e("dengying","JSONException");
					callbackContext.error("Wifi Init Error!");
				}
			}}
		);
	}

	private void sendPsnToDevice(String psn){
    Log.e("dengying", "sendPsnToDevice psn=" + psn);

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

            Log.e("dengying", "sendPsnToDevice Exception");
		}
	}

	private boolean checkTdmecParam(JSONArray args, CallbackContext callbackContext){
		try {
			String ssid = args.getString(0);
			String password = args.getString(1);
			String psn = args.getString(2);
			String serviceIp = args.getString(3);
			String port = args.getString(4);

			if (isEmpty(ssid)){
				callbackContext.error("请输入wifi名称!");
				return false;
			}

			if (isEmpty(password)){
				callbackContext.error("请输入wifi密码!");
				return false;
			}

			if (isEmpty(psn)){
				callbackContext.error("请输入psn!");
				return false;
			}

			if (isEmpty(serviceIp)){
				callbackContext.error("请输入IP!");
				return false;
			}

			if (isEmpty(port)){
				callbackContext.error("请输入port!");
				return false;
			}

			Log.e("dengying", "checkTdmecParam ssid=" + ssid + ",password=" + password + ",psn=" + psn + ",serviceIp=" + serviceIp + ",port=" + port);

		} catch (JSONException e) {
			e.printStackTrace();
			callbackContext.error("参数错误!");
			return false;
		}
		return true;
	}

	private boolean isEmpty(String string){
		if (string != null && string.trim().length() != 0){
			return false;
		}
		return true;
	}
}
