#import <Foundation/Foundation.h>
#import "CustomPathHTTPConnection.h"
#import "HTTPConnection.h"
#import "HTTPLogging.h"
#import "HTTPResponse.h"
#import "HTTPErrorResponse.h"
#import "HTTPDataResponse.h"

@implementation CustomPathHTTPConnection : HTTPConnection
static const int httpLogLevel = HTTP_LOG_LEVEL_WARN; // | HTTP_LOG_FLAG_TRACE;
static NSDictionary * customPaths = nil;
+ (NSDictionary *) customPaths { @synchronized(self) { return customPaths; } }
+ (void) setCustomPaths:(NSDictionary *) cusPaths { @synchronized(self) { customPaths = cusPaths; } }

/**
 * This method is called to get a response for a request.
 * You may return any object that adopts the HTTPResponse protocol.
 * The HTTPServer comes with two such classes: HTTPFileResponse and HTTPDataResponse.
 * HTTPFileResponse is a wrapper for an NSFileHandle object, and is the preferred way to send a file response.
 * HTTPDataResponse is a wrapper for an NSData object, and may be used to send a custom response.
 **/
- (NSObject<HTTPResponse> *)httpResponseForMethod:(NSString *)method URI:(NSString *)path
{
    HTTPLogTrace();
    
    __block NSObject<HTTPResponse> * httpResponseReturnable = nil;

    /*
     * The following block of code determins if the URL you have requested should be backended to the named
     * URL in the custom path. If it does, we'll make a synchronous request for it, and generate a
     * HTTPDataResponse object and pass that back. If the backend throws any errors, we'll generate a
     * HTTPErrorRepsonse object with the status code, and pass that back. If none of these match, we'll
     * allow the existing code to happen.
     */
    [CustomPathHTTPConnection.customPaths enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop)
     {
         NSString* customPath = (NSString*) key;
         if ([path hasPrefix:customPath] && ([obj hasPrefix:@"http://"] || [obj hasPrefix:@"https://"])) {
             *stop = YES;
             NSString* subPath = [path substringFromIndex:[customPath length]];
             NSURL *realURL = [NSURL URLWithString: [(NSString *) obj stringByAppendingString: subPath]];
             // turn it into a request and use NSData to load its content
             NSURLRequest *urlRequest = [NSURLRequest requestWithURL:realURL];
             NSHTTPURLResponse * urlResponse = nil;
             NSError * error = nil;
             NSData * data = [NSURLConnection sendSynchronousRequest:urlRequest returningResponse:&urlResponse error:&error];
             
             if (error != nil) {
                 httpResponseReturnable = [[HTTPErrorResponse alloc] initWithErrorCode: (int) urlResponse.statusCode];
             } else {
                 httpResponseReturnable = [[HTTPDataResponseWithHeaders alloc] initWithDataAndHeaders: data httpHeaders:urlResponse.allHeaderFields];
             }
         }
     }];
    
    if (httpResponseReturnable != nil)
    {
        return httpResponseReturnable;
    }
    else
    {
        return [super httpResponseForMethod:path URI:path];
    }

}


- (NSString *)filePathForURI:(NSString *)path allowDirectory:(BOOL)allowDirectory
{
    HTTPLogTrace();
    __block NSString *pathForURI = nil;
    [CustomPathHTTPConnection.customPaths enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop)
    {
        NSString* customPath = (NSString*) key;
        if ([path hasPrefix:customPath]) {
            *stop = YES;
            NSString* subPath = [path substringFromIndex:[customPath length]];
            pathForURI = [self filePathForURI:subPath allowDirectory:allowDirectory documentRoot:(NSString *) obj];
        }
    }];
    if (pathForURI != nil) {
        return pathForURI;
    } else {
        return [super filePathForURI:path allowDirectory:allowDirectory];
    }
}

- (NSString *)filePathForURI:(NSString *)path allowDirectory:(BOOL)allowDirectory documentRoot:(NSString *) documentRoot
{
    // Part 0: Validate document root setting.
    //
    // If there is no configured documentRoot,
    // then it makes no sense to try to return anything.
    
    if (documentRoot == nil)
    {
        HTTPLogWarn(@"%@[%p]: No configured document root", THIS_FILE, self);
        return nil;
    }
    
    // Part 1: Strip parameters from the url
    //
    // E.g.: /page.html?q=22&var=abc -> /page.html
    
    NSURL *docRoot = [NSURL fileURLWithPath:documentRoot isDirectory:YES];
    if (docRoot == nil)
    {
        HTTPLogWarn(@"%@[%p]: Document root is invalid file path", THIS_FILE, self);
        return nil;
    }
    
    NSString *relativePath = [[NSURL URLWithString:path relativeToURL:docRoot] relativePath];
    
    // Part 2: Append relative path to document root (base path)
    //
    // E.g.: relativePath="/images/icon.png"
    //       documentRoot="/Users/robbie/Sites"
    //           fullPath="/Users/robbie/Sites/images/icon.png"
    //
    // We also standardize the path.
    //
    // E.g.: "Users/robbie/Sites/images/../index.html" -> "/Users/robbie/Sites/index.html"
    
    NSString *fullPath = [[documentRoot stringByAppendingPathComponent:relativePath] stringByStandardizingPath];
    
    if ([relativePath isEqualToString:@"/"])
    {
        fullPath = [fullPath stringByAppendingString:@"/"];
    }
    
    // Part 3: Prevent serving files outside the document root.
    //
    // Sneaky requests may include ".." in the path.
    //
    // E.g.: relativePath="../Documents/TopSecret.doc"
    //       documentRoot="/Users/robbie/Sites"
    //           fullPath="/Users/robbie/Documents/TopSecret.doc"
    //
    // E.g.: relativePath="../Sites_Secret/TopSecret.doc"
    //       documentRoot="/Users/robbie/Sites"
    //           fullPath="/Users/robbie/Sites_Secret/TopSecret"
    
    if (![documentRoot hasSuffix:@"/"])
    {
        documentRoot = [documentRoot stringByAppendingString:@"/"];
    }
    
    if (![fullPath hasPrefix:documentRoot])
    {
        HTTPLogWarn(@"%@[%p]: Request for file outside document root", THIS_FILE, self);
        return nil;
    }
    
    // Part 4: Search for index page if path is pointing to a directory
    if (!allowDirectory)
    {
        BOOL isDir = NO;
        if ([[NSFileManager defaultManager] fileExistsAtPath:fullPath isDirectory:&isDir] && isDir)
        {
            NSArray *indexFileNames = [self directoryIndexFileNames];
            
            for (NSString *indexFileName in indexFileNames)
            {
                NSString *indexFilePath = [fullPath stringByAppendingPathComponent:indexFileName];
                
                if ([[NSFileManager defaultManager] fileExistsAtPath:indexFilePath isDirectory:&isDir] && !isDir)
                {
                    return indexFilePath;
                }
            }
            
            // No matching index files found in directory
            return nil;
        }
    }
    
    return fullPath;
}
@end

@implementation HTTPDataResponseWithHeaders : HTTPDataResponse
- (id)initWithDataAndHeaders:(NSData *)dataToUse httpHeaders:(NSDictionary *) httpHeaders
{
    if((self = [super initWithData: dataToUse]))
    {
        headers = [NSMutableDictionary dictionaryWithDictionary:httpHeaders];
        [(NSMutableDictionary *) headers removeObjectForKey:@"Content-Encoding"];
        [(NSMutableDictionary *) headers removeObjectForKey:@"Content-Length"];
    }
    return self;
}

- (NSDictionary *)httpHeaders
{
    return headers;
}
@end