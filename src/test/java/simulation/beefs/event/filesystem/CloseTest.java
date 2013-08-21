package simulation.beefs.event.filesystem;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.HashSet;
import java.util.Set;

import manelsim.EventScheduler;
import manelsim.EventSourceMultiplexer;
import manelsim.Time;
import manelsim.Time.Unit;

import org.junit.Before;
import org.junit.Test;

import simulation.beefs.model.DataServer;
import simulation.beefs.model.FileSystemClient;
import simulation.beefs.model.Machine;
import simulation.beefs.model.MetadataServer;
import simulation.beefs.model.ReplicatedFile;
import simulation.beefs.placement.DataPlacement;
import simulation.beefs.replication.Faithful;
import simulation.beefs.replication.Replicator;

public class CloseTest {
	
	private static final long TERABYTE = 1024 * 1024 * 1024 * 1024;

	private static final Time TO_SLEEP_TIMEOUT = new Time(15*60, Unit.SECONDS);
	private static final Time TRANSITION_DURATION = new Time(2500, Unit.MILLISECONDS);
	
	private String filePath = "/home/patrick/zerooo.txt";
	private EventSourceMultiplexer eventSourceMock;
	private Time closeTime = Time.GENESIS;
	private Time timeToCoherence = new Time(5 * 60, Unit.SECONDS);
	
	@Before
	public void setup() {
		eventSourceMock = createMock(EventSourceMultiplexer.class);
		EventScheduler.setup(Time.GENESIS, new Time(Long.MAX_VALUE, Unit.MICROSECONDS), eventSourceMock);
	}
	
	@Test
	public void testCloseNonModifiedFile() {
		Machine jurupoca = new Machine("jurupoca", TO_SLEEP_TIMEOUT, TRANSITION_DURATION);
		
		Set<DataServer> dataServers = new HashSet<DataServer>();
		dataServers.add(new DataServer(jurupoca, TERABYTE));
		DataPlacement dataPlacementAlgorithm = DataPlacement.newDataPlacement(DataPlacement.RANDOM, dataServers);
		Replicator replicator = new Faithful();
		MetadataServer metadataServer = new MetadataServer(dataServers, dataPlacementAlgorithm, replicator, 0, timeToCoherence);
		FileSystemClient client = new FileSystemClient(jurupoca, metadataServer, true);
		
		client.createOrOpen(filePath);
		
		replay(eventSourceMock);
		Close close = new Close(client, closeTime, filePath);
		close.process();
	}
	
	@Test
	public void testCloseNonModifiedFileWithReplicas() {
		Machine jurupoca = new Machine("jurupoca", TO_SLEEP_TIMEOUT, TRANSITION_DURATION);
		Machine cherne = new Machine("cherne", TO_SLEEP_TIMEOUT, TRANSITION_DURATION);
		Machine pepino = new Machine("pepino", TO_SLEEP_TIMEOUT, TRANSITION_DURATION);
		
		Set<DataServer> dataServers = new HashSet<DataServer>();
		dataServers.add(new DataServer(jurupoca, TERABYTE));
		dataServers.add(new DataServer(cherne, TERABYTE));
		dataServers.add(new DataServer(pepino, TERABYTE));
		DataPlacement dataPlacementAlgorithm = DataPlacement.newDataPlacement(DataPlacement.RANDOM, dataServers);
		Replicator replicator = new Faithful();
		MetadataServer metadataServer = new MetadataServer(dataServers, dataPlacementAlgorithm, replicator, 2, timeToCoherence);
		FileSystemClient client = new FileSystemClient(jurupoca, metadataServer, true);
		
		client.createOrOpen(filePath);
		
		replay(eventSourceMock);
		Close close = new Close(client, closeTime, filePath);
		close.process();
	}
	
	@Test
	public void testCloseModifiedFileWithReplicas() {
		Machine jurupoca = new Machine("jurupoca", TO_SLEEP_TIMEOUT, TRANSITION_DURATION);
		Machine cherne = new Machine("cherne", TO_SLEEP_TIMEOUT, TRANSITION_DURATION);
		Machine pepino = new Machine("pepino", TO_SLEEP_TIMEOUT, TRANSITION_DURATION);
		
		Set<DataServer> dataServers = new HashSet<DataServer>();
		dataServers.add(new DataServer(jurupoca, TERABYTE));
		dataServers.add(new DataServer(cherne, TERABYTE));
		dataServers.add(new DataServer(pepino, TERABYTE));
		DataPlacement dataPlacementAlgorithm = DataPlacement.newDataPlacement(DataPlacement.RANDOM, dataServers);
		Replicator replicator = new Faithful();
		MetadataServer metadataServer = new MetadataServer(dataServers, dataPlacementAlgorithm, replicator, 2, timeToCoherence);
		FileSystemClient client = new FileSystemClient(jurupoca, metadataServer, true);
		
		ReplicatedFile file = client.createOrOpen(filePath);
		file.write(10, 1000);
		
		eventSourceMock.addNewEvent(new UpdateFileReplicas(closeTime.plus(timeToCoherence), file, metadataServer));
		replay(eventSourceMock);
		
		Close close = new Close(client, closeTime, filePath);
		close.process();
		
		verify(eventSourceMock);
	}
	
	@Test
	public void testCloseNonExistentFile() {
		Machine jurupoca = new Machine("jurupoca", TO_SLEEP_TIMEOUT, TRANSITION_DURATION);
		
		Set<DataServer> dataServers = new HashSet<DataServer>();
		dataServers.add(new DataServer(jurupoca, TERABYTE));
		DataPlacement dataPlacementAlgorithm = DataPlacement.newDataPlacement(DataPlacement.RANDOM, dataServers);
		Replicator replicator = new Faithful();
		MetadataServer metadataServer = new MetadataServer(dataServers, dataPlacementAlgorithm, replicator, 0, timeToCoherence);
		FileSystemClient client = new FileSystemClient(jurupoca, metadataServer, true);
		
		replay(eventSourceMock);
		Close close = new Close(client, closeTime, filePath);
		close.process();
	}
}
