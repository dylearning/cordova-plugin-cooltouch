//
//  CDVEasyLink.m
//  HelloCordova
//
//  Created by Thomas.Wang on 2016/11/14.
//
//

#import "CDVEasyLink.h"

@implementation CDVEasyLink
-(void)pluginInitialize{
    m_easylink_config = [[EASYLINK alloc]initWithDelegate:self];
    

}

- (void)getWifiSSid:(CDVInvokedUrlCommand*)command
{
    NSString *ssidString = [EASYLINK ssidForConnectedNetwork];
    
    

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:ssidString];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

}
-(void)startSearch:(CDVInvokedUrlCommand*)command
{
    m_command= command;
    NSString* ssidString = [command.arguments objectAtIndex:0];
    NSString* passwordString = [command.arguments objectAtIndex:1];
    NSString* type = [command.arguments objectAtIndex:2];
    
    
    if (![self checkTdmecParam:ssidString :passwordString :type]) {
        return;
    }
    NSMutableDictionary *wlanConfig = [NSMutableDictionary dictionaryWithCapacity:20];
    if( m_easylink_config == nil){
        m_easylink_config = [[EASYLINK alloc]initWithDelegate:self];
    }
    [m_easylink_config stopTransmitting];
    NSData* ssidData = [ssidString dataUsingEncoding:NSUTF8StringEncoding];
    
    [wlanConfig setObject:ssidData forKey:KEY_SSID];
    [wlanConfig setObject:passwordString forKey:KEY_PASSWORD];
    [wlanConfig setObject:[NSNumber numberWithBool:YES] forKey:KEY_DHCP];

   if([@"SWITCH" isEqualToString:type]){
        NSString* psn = [command.arguments objectAtIndex:3];
        NSString* serviceIp = [command.arguments objectAtIndex:4];
        NSString* portString =[command.arguments objectAtIndex:5];
        if(![self checkSwitchParam:ssidString :passwordString :type :psn :serviceIp :portString]){
            return ;
        }
        m_psn = psn;
        int port = [portString intValue];
       NSMutableDictionary *jsonDict = [[NSMutableDictionary alloc] init];
       [jsonDict setValue:serviceIp forKey:@"host"];
       [jsonDict setValue:[NSNumber numberWithUnsignedLong:port] forKey:@"port"];
       NSString *jsonStr = [self dictionaryToJson:jsonDict];
       const char *temp = [jsonStr cStringUsingEncoding:NSUTF8StringEncoding];
//        const char *temp = [@"{\"host\":\"192.168.14.215\",\"port\":9778}" cStringUsingEncoding:NSUTF8StringEncoding];
      

        [m_easylink_config prepareEasyLink_withFTC:wlanConfig info:[NSData dataWithBytes:temp length:strlen(temp)] mode:EASYLINK_V2_PLUS ];
   }  else  {
       [m_easylink_config prepareEasyLink_withFTC:wlanConfig info:nil mode:EASYLINK_V2_PLUS];
       udpSocket = [[AsyncUdpSocket alloc] initWithDelegate:self];
       NSError *err = nil;
       [udpSocket enableBroadcast:YES error:&err];
       
       [udpSocket bindToPort:20001 error:&err];
       //启动接收线程
       [udpSocket receiveWithTimeout:-1 tag:0];
       
   }
    [m_easylink_config transmitSettings];


}
-(Boolean)checkTdmecParam:(NSString*) ssidString :(NSString*) passwordString:(NSString*) type{
    if ([self isBlankString:ssidString]) {
        [self callbackError:@"请输入wifi名称！"];
        return NO;
    }
    if ([self isBlankString:passwordString]) {
        [self callbackError:@"请输入wifi密码！"];
        return NO;
    }
//    if ([self isBlankString:type]) {
//        [self callbackError:@"请输入类型！"];
//        return NO;
//    }
    
    return YES;
    
}
-(Boolean)checkSwitchParam:(NSString*) ssidString :(NSString*) passwordString:(NSString*)type:(NSString*) psn:(NSString*) serviceIp:(NSString*) portString{
//    if ([self isBlankString:ssidString]) {
//        [self callbackError:@"请输入wifi名称！"];
//        return NO;
//    }
//    if ([self isBlankString:passwordString]) {
//        [self callbackError:@"请输入wifi密码！"];
//        return NO;
//    }
//    
//    if ([self isBlankString:type]) {
//        [self callbackError:@"请输入类型！"];
//        return NO;
//    }
    if ([self isBlankString:psn]) {
        [self callbackError:@"请输入psn！"];
        return NO;
    }
    if ([self isBlankString:serviceIp]) {
        [self callbackError:@"请输入服务器的ip！"];
        return NO;
    }
    if ([self isBlankString:portString]) {
        [self callbackError:@"请输入服务器的端口！"];
        return NO;
    }
   
    return YES;
    
}
-(void)callbackError:(NSString*)reason{
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:reason];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:m_command.callbackId];
}
- (BOOL) isBlankString:(NSString *)string {
    if (string == nil || string == NULL) {
        return YES;
    }
    if ([string isKindOfClass:[NSNull class]]) {
        return YES;
    }
    if ([[string stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]] length]==0) {
        return YES;
    }
    return NO;
}
-(void)stopSearch:(CDVInvokedUrlCommand*)command
{
    //停止EasyLink，注意释放内存
    NSLog(@"stopTransmitting");
    [m_easylink_config stopTransmitting];
    [m_easylink_config unInit];
    m_easylink_config=nil;
    [self closeUdpSocket];
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"停止搜索"];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

}

-(void) closeUdpSocket{

    if(udpSocket !=NULL){
        [udpSocket close];
        udpSocket = nil;
    }
}

//注意：有些庆科模块的固件代码有回连机制，配置成功设备会返回数据给app，新版本设备配上网络以后是不回连的
#pragma mark - EasyLink delegate -
- (void)onFoundByFTC:(NSNumber *)ftcClientTag withConfiguration: (NSDictionary *)configDict
{
    NSLog(@"New device found!");
//    [m_easylink_config configFTCClient:ftcClientTag
//                     withConfiguration: [NSDictionary dictionary] ];
    [m_easylink_config configFTCClient:ftcClientTag
                     withConfiguration: @{@"access_key":m_psn} ];
    NSString *jsonStr =[self dictionaryToJson:configDict];
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:jsonStr];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:m_command.callbackId];
    
}
#pragma mark - EasyLink delegate -
- (void)onDisconnectFromFTC:(NSNumber *)ftcClientTag
{
    NSLog(@"Device disconnected!");
}

#pragma mark - UIAlertViewDelegate -
- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex{
    //停止EasyLink，注意释放内存
    NSLog(@"stopTransmitting");
    [m_easylink_config stopTransmitting];
    [m_easylink_config unInit];
    m_easylink_config=nil;
}

//已接收到消息
- (BOOL)onUdpSocket:(AsyncUdpSocket *)sock didReceiveData:(NSData *)data withTag:(long)tag fromHost:(NSString *)host port:(UInt16)port{
   
    Byte *receiveByte = (Byte *)[data bytes];
    NSUInteger len = [data length];
    Byte *psnByte = (Byte*)malloc(8);
    Byte *ipByte = (Byte *)malloc(len-8);
    [self bytesplit2byte:receiveByte orc:psnByte begin:0 count:8];
    [self bytesplit2byte:receiveByte orc:ipByte begin:8 count:(len-8)];
    long psn = [self lBytesToLong:psnByte];
    NSString* ip = [NSString stringWithUTF8String: (char *)ipByte];
    ip = [ip stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
    NSMutableDictionary *jsonDict = [[NSMutableDictionary alloc] init];
    [jsonDict setValue:[NSNumber numberWithUnsignedLong:psn] forKey:@"psn"];
    [jsonDict setValue:ip forKey:@"ip"];
    NSString *jsonStr = [self dictionaryToJson:jsonDict];
    NSLog(@"jsonStr == %@",jsonStr);
    
    [self closeUdpSocket];
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:jsonStr];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:m_command.callbackId];
    return YES;
}

//字节数组的分割
- (void)bytesplit2byte:(Byte[])src orc:(Byte[])orc begin:(NSInteger)begin count:(NSInteger)count{
    memset(orc, 0, sizeof(char)*count);
    for (NSInteger i = begin; i < begin+count; i++){
        orc[i-begin] = src[i];
    }
}
//字节转换成Long
-(long) lBytesToLong:(Byte[]) byte
{
    long height = 0;
    NSData * testData =[NSData dataWithBytes:byte length:8];
    for (int i = 0; i < [testData length]; i++)
    {
        if (byte[[testData length]-i] >= 0)
        {
            height = height + byte[[testData length]-i];
        } else
        {
            height = height + 256 + byte[[testData length]-i];
        }
        height = height * 256;
    }
    if (byte[0] >= 0)
    {
        height = height + byte[0];
    } else {
        height = height + 256 + byte[0];
    }
    return height;
}

//字节转换成String
- (NSString *)byteToString:(Byte*)b{
    int len = sizeof(b);
//    NSData * testData =[NSData dataWithBytes:b length:8];
    NSData *adata = [[NSData alloc] initWithBytes:b length:len];
    
    NSLog(@"%@",adata);
    
    NSString *aString = [[NSString alloc]initWithData:adata encoding:NSUTF8StringEncoding];
    
    NSLog(@"%@",aString);
    return aString;
    
}

- (NSString*)dictionaryToJson:(NSDictionary *)dic

{
    
    NSError *parseError = nil;
    
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:dic options:NSJSONWritingPrettyPrinted error:&parseError];
    
    return [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
    
}

//没有接受到消息
-(void)onUdpSocket:(AsyncUdpSocket *)sock didNotReceiveDataWithTag:(long)tag dueToError:(NSError *)error{
    
}
//没有发送出消息
-(void)onUdpSocket:(AsyncUdpSocket *)sock didNotSendDataWithTag:(long)tag dueToError:(NSError *)error{
    
}
//已发送出消息
-(void)onUdpSocket:(AsyncUdpSocket *)sock didSendDataWithTag:(long)tag{
    
}
//断开连接
-(void)onUdpSocketDidClose:(AsyncUdpSocket *)sock{
    
}

@end
