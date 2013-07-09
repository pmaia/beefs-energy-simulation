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
package simulation.beefs.placement;

import java.util.Set;

import simulation.beefs.model.DataServer;
import simulation.beefs.model.FileSystemClient;
import simulation.beefs.model.ReplicatedFile;

/**
 * Encapsulates the logic of {@link ReplicationGroup} creation, deciding where primary
 * and secondaries replicas of a file are placed.
 * 
 * @author thiagoepdc - thiagoepdc@lsd.ufcg.edu.br
 */
public abstract class DataPlacement {
	
	public static final String RANDOM = "random";
	public static final String CO_RANDOM = "co-random";
	
	protected Set<DataServer> dataServers;
	
	public DataPlacement(Set<DataServer> dataServers) {
		this.dataServers = dataServers;
	}
	
	public static DataPlacement newDataPlacement(String type, Set<DataServer> dataServers) {
		if(CO_RANDOM.equals(type)) {
			return new CoLocatedWithSecondaryRandom(dataServers);
		} else if(RANDOM.equals(type)) {
			return new Random(dataServers);
		} else {
			throw new IllegalArgumentException(type + " is not a valid DataPlacementAlgorithm type.");
		}
	}

	public abstract ReplicatedFile createFile(FileSystemClient client, String fullpath, int replicationLevel);
	
}