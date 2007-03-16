//
//  MQSVNUpdateJob.m
//  Mindquarry Desktop Client
//
//  Created by Jonas on 15.03.07.
//  Copyright 2007 __MyCompanyName__. All rights reserved.
//

#import "MQSVNUpdateJob.h"

#import "MQTeam.h"
#import "MQServer.h"
#import "Mindquarry_Desktop_Client_AppDelegate.h"
#import "SVNController.h"

@implementation MQSVNUpdateJob

- (void)threadMethod
{
	NSEnumerator *teamEnum = [[server valueForKey:@"teams"] objectEnumerator];
	id team;
	while (team = [teamEnum nextObject]) {
		[team initJVM];
		[[team svnController] attachCurrentThread];
		
		[team update];
		[team getSVNChanges];
	}
	
	[[NSApp delegate] performSelectorOnMainThread:@selector(reloadChanges) withObject:nil waitUntilDone:YES];
	[self performSelectorOnMainThread:@selector(finishRequest) withObject:nil waitUntilDone:YES];
}

@end
