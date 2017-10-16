# ionic-mico-sdk
easyLink插件
## Installation

    cordova plugin add https://github.com/GTDistance/ionic-mico-sdk.git
    
## Uninstall
    cordova plugin rm ionic-mico-sdk
    
### Supported Platforms

- Android
- iOS

### Quick Example
    //获取wifi名,成功之后会返回wifi的名称,失败会返回失败的原因
    easyLink.getWifiSSid(function(message){
      alert(message);
    },function(message){
      alert(message);
    });
    
    //开始配网,配网成功如果是EasyLink会返回ip和psn的jsonString,如果是EasyLink_CT会返回device_id和type_id的jsonString。
    //参数说明:
    //wifi名称,wifi对应的密码,设备通信协议type,设备psn,要连接服务器的serviceIp,要连接服务器的port;所有的都是String类型。
    //示例代码如下:
    
    //EasyLink_CT类型 所有参数不可为null。
    easyLink.startSearch("wifiSSid", "wifiPsw","EasyLink_CT","88740009","0.0.0.0","0000", function (message) {
      alert(message);
    }, function (message) {
      alert(message);
    });
    
    //EasyLink类型  wifi名称和wifi密码不能为null,其他参数可以为null。
    easyLink.startSearch("wifiSSid", "wifiPsw",null,null,null,null, function (message) {
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
