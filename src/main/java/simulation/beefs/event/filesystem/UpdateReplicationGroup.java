/**
 * Copyright (C) 2009 Universidade Federal de Campina Grande
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package simulation.beefs.event.filesystem;

import simulation.beefs.MetadataServer;
import core.Event;
import core.Time;

/**
 *
 * @author Patrick Maia - patrickjem@lsd.ufcg.edu.br
 */
public class UpdateReplicationGroup extends Event {
	
	private final String filePath;
	private final MetadataServer metadataServer;

	public UpdateReplicationGroup(MetadataServer metadataServer, Time aScheduledTime, String filePath) {
		super(aScheduledTime);
		
		this.metadataServer = metadataServer;
		this.filePath = filePath;
	}
	
	public void process() {
		//TODO implement
	}
	
	public String getFilePath() {
		return this.filePath;
	}
	
	@Override
	public String toString() {
		return metadataServer + "\tupdate replication group\t" + getScheduledTime() + "\t" + filePath;
	}

}
