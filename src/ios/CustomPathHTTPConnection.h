#import "HTTPConnection.h"

@interface CustomPathHTTPConnection : HTTPConnection
+ (NSDictionary *) customPaths;
+ (void) setCustomPaths:(NSDictionary *) cusPaths;
- (NSString *)filePathForURI:(NSString *)path allowDirectory:(BOOL)allowDirectory documentRoot:(NSString *) documentRoot;
@end