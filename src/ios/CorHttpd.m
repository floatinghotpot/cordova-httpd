/********* CDVCorHttpd.m Cordova Plugin Implementation *******/

#import <Cordova/CDV.h>

#import "DDLog.h"
#import "DDTTYLogger.h"
#import "HTTPServer.h"

@interface CorHttpd : CDVPlugin {
    // Member variables go here.

}

@property(nonatomic, retain) HTTPServer *httpServer;
@property(nonatomic, retain) NSString *localPath;
@property(nonatomic, retain) NSString *url;
@property (assign) int port;

- (void)startServer:(CDVInvokedUrlCommand*)command;
- (void)stopServer:(CDVInvokedUrlCommand*)command;
- (void)getURL:(CDVInvokedUrlCommand*)command;
- (void)getLocalPath:(CDVInvokedUrlCommand*)command;

- (NSDictionary *)getIPAddresses;
- (NSString *)getIPAddress:(BOOL)preferIPv4;

@end

@implementation CorHttpd

#include <ifaddrs.h>
#include <arpa/inet.h>
#include <net/if.h>

#define IOS_CELLULAR    @"pdp_ip0"
#define IOS_WIFI        @"en0"
#define IP_ADDR_IPv4    @"ipv4"
#define IP_ADDR_IPv6    @"ipv6"

- (NSString *)getIPAddress:(BOOL)preferIPv4
{
    NSArray *searchArray = preferIPv4 ?
    @[ IOS_WIFI @"/" IP_ADDR_IPv4, IOS_WIFI @"/" IP_ADDR_IPv6, IOS_CELLULAR @"/" IP_ADDR_IPv4, IOS_CELLULAR @"/" IP_ADDR_IPv6 ] :
    @[ IOS_WIFI @"/" IP_ADDR_IPv6, IOS_WIFI @"/" IP_ADDR_IPv4, IOS_CELLULAR @"/" IP_ADDR_IPv6, IOS_CELLULAR @"/" IP_ADDR_IPv4 ] ;
    
    NSDictionary *addresses = [self getIPAddresses];
    NSLog(@"addresses: %@", addresses);
    
    __block NSString *address;
    [searchArray enumerateObjectsUsingBlock:^(NSString *key, NSUInteger idx, BOOL *stop)
     {
         address = addresses[key];
         if(address) *stop = YES;
     } ];
    return address ? address : @"0.0.0.0";
}

- (NSDictionary *)getIPAddresses
{
    NSMutableDictionary *addresses = [NSMutableDictionary dictionaryWithCapacity:8];
    
    // retrieve the current interfaces - returns 0 on success
    struct ifaddrs *interfaces;
    if(!getifaddrs(&interfaces)) {
        // Loop through linked list of interfaces
        struct ifaddrs *interface;
        for(interface=interfaces; interface; interface=interface->ifa_next) {
            if(!(interface->ifa_flags & IFF_UP) /* || (interface->ifa_flags & IFF_LOOPBACK) */ ) {
                continue; // deeply nested code harder to read
            }
            const struct sockaddr_in *addr = (const struct sockaddr_in*)interface->ifa_addr;
            char addrBuf[ MAX(INET_ADDRSTRLEN, INET6_ADDRSTRLEN) ];
            if(addr && (addr->sin_family==AF_INET || addr->sin_family==AF_INET6)) {
                NSString *name = [NSString stringWithUTF8String:interface->ifa_name];
                NSString *type;
                if(addr->sin_family == AF_INET) {
                    if(inet_ntop(AF_INET, &addr->sin_addr, addrBuf, INET_ADDRSTRLEN)) {
                        type = IP_ADDR_IPv4;
                    }
                } else {
                    const struct sockaddr_in6 *addr6 = (const struct sockaddr_in6*)interface->ifa_addr;
                    if(inet_ntop(AF_INET6, &addr6->sin6_addr, addrBuf, INET6_ADDRSTRLEN)) {
                        type = IP_ADDR_IPv6;
                    }
                }
                if(type) {
                    NSString *key = [NSString stringWithFormat:@"%@/%@", name, type];
                    addresses[key] = [NSString stringWithUTF8String:addrBuf];
                }
            }
        }
        // Free memory
        freeifaddrs(interfaces);
    }
    return [addresses count] ? addresses : nil;
}

- (void)pluginInitialize
{
    self.httpServer = nil;
    self.localPath = @"";
    self.url = @"";
}

- (void)startServer:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;
    NSString* wwwRoot = [command.arguments objectAtIndex:0];
    int port = [[command.arguments objectAtIndex:1] intValue];

    if(self.httpServer != nil) {
        if([self.httpServer isRunning]) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"server is already up"];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            return;
        }
    }
    
    if (wwwRoot == nil) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"wwwRoot is missing"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    
    [DDLog addLogger:[DDTTYLogger sharedInstance]];
    self.httpServer = [[HTTPServer alloc] init];
    
    // Tell the server to broadcast its presence via Bonjour.
    // This allows browsers such as Safari to automatically discover our service.
    //[self.httpServer setType:@"_http._tcp."];
    
    // Normally there's no need to run our server on any specific port.
    // Technologies like Bonjour allow clients to dynamically discover the server's port at runtime.
    // However, for easy testing you may want force a certain port so you can just hit the refresh button.
    // [httpServer setPort:12345];
    
    [self.httpServer setPort:port];
    
    // Serve files from our embedded Web folder
    const char * str = [wwwRoot UTF8String];
    if(*str == '/') {
        self.localPath = wwwRoot;
    } else {
        NSString* basePath = [[[NSBundle mainBundle] resourcePath] stringByAppendingPathComponent:@"www"];
        self.localPath = [NSString stringWithFormat:@"%@/%@", basePath, wwwRoot];
    }
    NSLog(@"Setting document root: %@", self.localPath);
    [self.httpServer setDocumentRoot:self.localPath];
    
	NSError *error;
	if([self.httpServer start:&error]) {
        int listenPort = [self.httpServer listeningPort];
		NSLog(@"Started httpd on port %d", listenPort);
        
        NSString* ip = [self getIPAddress:YES];
        self.url = [NSString stringWithFormat:@"http://%@:%d/", ip, listenPort];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:self.url];
        
	} else {
		NSLog(@"Error starting httpd: %@", error);
        
        NSString* errmsg = [error description];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:errmsg];
	}
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)stopServer:(CDVInvokedUrlCommand*)command
{
    if(self.httpServer != nil) {
        
        [self.httpServer stop];
        self.httpServer = nil;
        
        self.localPath = @"";
        self.url = @"";
        
        NSLog(@"httpd stopped");
    }
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)getURL:(CDVInvokedUrlCommand *)command
{
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                      messageAsString:(self.url ? self.url : @"")];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)getLocalPath:(CDVInvokedUrlCommand *)command
{
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                      messageAsString:(self.localPath ? self.localPath : @"")];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end

