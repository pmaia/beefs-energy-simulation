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

/**
 * @author Patrick Maia - patrickjem@lsd.ufcg.edu.br
 */
public class CloseTest {

	private static final Time TO_SLEEP_TIMEOUT = new Time(15*60, Unit.SECONDS);
	private static final Time TRANSITION_DURATION = new Time(2500, Unit.MILLISECONDS);
	
	private String filePath = "/home/patrick/zerooo.txt";
	private EventSourceMultiplexer eventSourceMock;
	private Time closeTime = Time.GENESIS;
	private Time timeToCoherence = new Time(5 * 60, Unit.SECONDS);
	private Time timeToDelete  = new Time(5 * 60, Unit.SECONDS);
	
	@Before
	public void setup() {
		eventSourceMock = createMock(EventSourceMultiplexer.class);
		EventScheduler.setup(Time.GENESIS, new Time(Long.MAX_VALUE, Unit.MICROSECONDS), eventSourceMock);
	}
	
	@Test
	public void testCloseNonModifiedFile() {
		Machine jurupoca = new Machine("jurupoca", TO_SLEEP_TIMEOUT, TRANSITION_DURATION);
		
		Set<DataServer> dataServers = new HashSet<DataServer>();
		dataServers.add(new DataServer(jurupoca));
		boolean wakeOnLan = true;
		DataPlacement dataPlacementAlgorithm = DataPlacement.newDataPlacement(DataPlacement.RANDOM, dataServers);
		MetadataServer metadataServer = new MetadataServer(dataServers, dataPlacementAlgorithm, 0, timeToCoherence, timeToDelete, wakeOnLan);
		FileSystemClient client = new FileSystemClient(jurupoca, metadataServer, wakeOnLan);
		
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
		dataServers.add(new DataServer(jurupoca));
		dataServers.add(new DataServer(cherne));
		dataServers.add(new DataServer(pepino));
		boolean wakeOnLan = true;
		DataPlacement dataPlacementAlgorithm = DataPlacement.newDataPlacement(DataPlacement.RANDOM, dataServers);
		MetadataServer metadataServer = new MetadataServer(dataServers, dataPlacementAlgorithm, 2, timeToCoherence, timeToDelete, wakeOnLan);
		FileSystemClient client = new FileSystemClient(jurupoca, metadataServer, wakeOnLan);
		
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
		dataServers.add(new DataServer(jurupoca));
		dataServers.add(new DataServer(cherne));
		dataServers.add(new DataServer(pepino));
		boolean wakeOnLan = true;
		DataPlacement dataPlacementAlgorithm = DataPlacement.newDataPlacement(DataPlacement.RANDOM, dataServers);
		MetadataServer metadataServer = new MetadataServer(dataServers, dataPlacementAlgorithm, 2, timeToCoherence, timeToDelete, wakeOnLan);
		FileSystemClient client = new FileSystemClient(jurupoca, metadataServer, wakeOnLan);
		
		ReplicatedFile file = client.createOrOpen(filePath);
		file.setReplicasAreConsistent(false);
		
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
		dataServers.add(new DataServer(jurupoca));
		boolean wakeOnLan = true;
		DataPlacement dataPlacementAlgorithm = DataPlacement.newDataPlacement(DataPlacement.RANDOM, dataServers);
		MetadataServer metadataServer = new MetadataServer(dataServers, dataPlacementAlgorithm, 0, timeToCoherence, timeToDelete, wakeOnLan);
		FileSystemClient client = new FileSystemClient(jurupoca, metadataServer, wakeOnLan);
		
		replay(eventSourceMock);
		Close close = new Close(client, closeTime, filePath);
		close.process();
	}
}
