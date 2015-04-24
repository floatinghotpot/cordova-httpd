#import <Foundation/Foundation.h>
#import "HTTPConnection.h"
#import "HTTPLogging.h"

@implementation CustomPathHTTPConnection : HTTPConnection
static const int httpLogLevel = HTTP_LOG_LEVEL_WARN; // | HTTP_LOG_FLAG_TRACE;
static NSDictionary * customPaths = nil;
+ (NSDictionary *) customPaths { @synchronized(self) { return customPaths; } }
+ (void) setCustomPaths:(NSDictionary *) cusPaths { @synchronized(self) { customPaths = cusPaths; } }

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