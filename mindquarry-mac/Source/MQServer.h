//
//  MQServer.h
//  Mindquarry Desktop Client
//
//  Created by Jonas on 08.03.07.
//  Copyright 2007 __MyCompanyName__. All rights reserved.
//

#import <Cocoa/Cocoa.h>

#import "MQRequest.h"

@interface MQServer : NSManagedObject {

	NSMutableArray *requestQueue;
	NSLock *requestLock;
	int requestRunningCount;
	
	NSLock *runningLock;
	NSMutableArray *runningRequests;
	
	NSURLCredential *credential;
	NSURLProtectionSpace *protectionSpace;
	
}

- (void)initRequestQueue;

- (void)enqueueRequest:(MQRequest *)req;

- (void)runFromQueueIfNeeded:(id)sender;

- (NSMutableArray *)requestQueue;

- (NSLock *)requestLock;

- (void)cancelAll;

- (void)setUsername:(NSString *)username;

- (void)setPassword:(NSString *)password;

- (void)clearCredentials;

@end