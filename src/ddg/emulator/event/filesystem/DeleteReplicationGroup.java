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
package ddg.emulator.event.filesystem;

import ddg.kernel.Event;
import ddg.kernel.EventHandler;
import ddg.kernel.Time;

/**
 * TODO make doc
 *
 * @author Patrick Maia - patrickjem@lsd.ufcg.edu.br
 */
public class DeleteReplicationGroup extends Event {
	
	public static final String EVENT_NAME = "delete-replication-group";
	
	private final String filePath;

	public DeleteReplicationGroup(EventHandler aHandler, Time aScheduledTime, String filePath) {
		super(EVENT_NAME, aHandler, aScheduledTime);
		
		this.filePath = filePath;
	}
	
	public String getFilePath() { 
		return this.filePath;
	}
	
	@Override
	public String toString() {
		return EVENT_NAME + "\t" + getScheduledTime() + "\t" + filePath;
	}

}