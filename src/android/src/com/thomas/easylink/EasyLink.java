package com.thomas.easylink;

import android.content.Context;
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
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


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
	private UDPServer server;
	private Context ctx = null;
	@Override
	protected void pluginInitialize() {
		super.pluginInitialize();
		ctx = cordova.getActivity();
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
			if ("SWITCH".equals(type)){
				ftcService(args, callbackContext);
			}else {
				easyLinkService(args, callbackContext);
			}
			return true;
		} else if ("stopSearch".equals(action)) {
			if(ftcService != null) {
				ftcService.stopTransmitting();
			}
			if (elapi!=null){
				elapi.stopEasyLink();
				server.setLife(false);
			}
			callbackContext.success("停止配网");
			return true;
		}
		return false; // Returning false results in a "MethodNotFound" error.
	}

	/**
	 * TMDEC的easylink
	 * @param args
	 * @param callbackContext
	 * @throws JSONException
	 */
	private void easyLinkService(JSONArray args, CallbackContext callbackContext) throws JSONException {
		String ssid = args.getString(0);
		String password = args.getString(1);
		if(elapi!=null) {
			elapi.stopEasyLink();
		}else {
			elapi = new EasyLinkAPI(cordova.getActivity());
		}
		if (server!=null){
			server.setLife(false);
		}else {
			server = new UDPServer();
		}
		elapi.startEasyLink(cordova.getActivity(),ssid,password);
		// 开启UDP服务器
		ExecutorService exec = Executors.newCachedThreadPool();
		server.setCallbackContextAndEasyLink(callbackContext,elapi);
		server.setLife(true);
		exec.execute(server);
	}

	private boolean checkTdmecParam(JSONArray args, CallbackContext callbackContext){
		try {
			String ssid = args.getString(0);
			String password = args.getString(1);
//			String type = args.getString(2);
			if (isEmpty(ssid)){
				callbackContext.error("请输入wifi名称!");
				return false;
			}
			if (isEmpty(password)){
				callbackContext.error("请输入wifi密码!");
				return false;
			}
//			if (isEmpty(type)){
//				callbackContext.error("请输入类型!");
//				return false;
//			}
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
