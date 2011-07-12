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
package ddg.emulator.events.metadataServerEvents;

import ddg.kernel.JEEvent;
import ddg.kernel.JEEventHandler;
import ddg.kernel.JETime;
import ddg.model.data.DataServer;

/**
 * TODO make doc
 * 
 */
public class MigrateEvent extends JEEvent {

	public static final String EVENT_NAME = "MIGRATE";
	private final String file;
	private final DataServer dataServer;

	/**
	 * @param file
	 * @param dataServer
	 * @param aHandler
	 * @param aScheduledTime
	 */
	public MigrateEvent(String file, DataServer dataServer,
			JEEventHandler aHandler, JETime aScheduledTime) {
		super(EVENT_NAME, aHandler, aScheduledTime);
		this.file = file;
		this.dataServer = dataServer;
	}

	/**
	 * @return
	 */
	public DataServer getDataServer() {
		return dataServer;
	}

	/**
	 * @return
	 */
	public String getFileName() {
		return file;
	}

}