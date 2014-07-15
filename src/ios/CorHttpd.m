/********* CDVCorHttpd.m Cordova Plugin Implementation *******/

#import <Cordova/CDV.h>

@interface CorHttpd : CDVPlugin {
    // Member variables go here.

    //@property(nonatomic, retain) WebServer *httpd;
    
    @property(nonatomic, retain) NSString *wwwRoot;
    @property (assign) int port;
}

- (void)startServer:(CDVInvokedUrlCommand*)command;
- (void)stopServer:(CDVInvokedUrlCommand*)command;
@end

@implementation CorHttpd

- (void)startServer:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;
    NSString* wwwRoot = [command.arguments objectAtIndex:0];
    int port = [[command.arguments objectAtIndex:1] intValue];

    if (wwwRoot != nil) {
        
        /* start web server */
        
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)stopServer:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;
    NSString* reservedArg = [command.arguments objectAtIndex:0];
    
    if (reservedArg != nil) {
        
        /* stop web server */
        
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end
