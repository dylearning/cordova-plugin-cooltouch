# cooltouch 智能开关面板
   橙朴(上海)智能科技有限公司
   ionic-mico-sdk easyLink插件

## Installation

    cordova plugin add https://github.com/dylearning/cordova-plugin-cooltouch.git

## Uninstall
   cordova plugin rm cordova-plugin-cooltouch
    
### Supported Platforms
- Android

### Quick Example
    //示例代码如下:

    //获取wifi名称
     easyLink.getWifiSSid(function(message){
        alert(message);
     },function(message){
        alert(message);
     });

    //开始配网,配网成功EasyLink_CT会返回device_id和type_id的jsonString。
    //参数说明:
    //wifi名称,wifi对应的密码,设备psn,要连接服务器的serviceIp,要连接服务器的port;所有的都是String类型
    //EasyLink_CT类型 所有参数不可为null
    easyLink.startSearch("wifiSSid", "wifiPsw","88740009","0.0.0.0","0000", function (message) {
      alert(message);
    }, function (message) {
      alert(message);
    });

    //停止搜索,成功返回"停止搜索",失败会返回失败原因。
    easyLink.stopSearch(function (message) {
      alert(message);
    }, function (message) {
      alert(message);
    });
