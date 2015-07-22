#import "HTTPConnection.h"
#import "HTTPDataResponse.h"

@interface CustomPathHTTPConnection : HTTPConnection
+ (NSDictionary *) customPaths;
+ (void) setCustomPaths:(NSDictionary *) cusPaths;
- (NSString *)filePathForURI:(NSString *)path allowDirectory:(BOOL)allowDirectory documentRoot:(NSString *) documentRoot;
- (NSObject<HTTPResponse> *)httpResponseForMethod:(NSString *)method URI:(NSString *)path;
@end

@interface HTTPDataResponseWithHeaders : HTTPDataResponse
{
    NSDictionary * headers;
}
- (id)initWithDataAndHeaders:(NSData *)data httpHeaders:(NSDictionary *) httpHeaders;
- (NSDictionary *)httpHeaders;
@end