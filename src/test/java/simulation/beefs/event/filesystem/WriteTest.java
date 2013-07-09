package simulation.beefs.event.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import manelsim.EventScheduler;
import manelsim.EventSource;
import manelsim.Time;
import manelsim.Time.Unit;

import org.junit.Before;
import org.junit.Test;

import simulation.beefs.event.machine.UserIdleness;
import simulation.beefs.model.DataServer;
import simulation.beefs.model.FileSystemClient;
import simulation.beefs.model.Machine;
import simulation.beefs.model.Machine.State;
import simulation.beefs.model.MetadataServer;
import simulation.beefs.model.ReplicatedFile;
import simulation.beefs.placement.DataPlacement;
import simulation.beefs.util.ObservableEventSourceMultiplexer;

public class WriteTest {
	
	private static final Time TO_SLEEP_TIMEOUT = new Time(15*60, Unit.SECONDS);
	private static final Time TRANSITION_DURATION = new Time(2500, Unit.MILLISECONDS);
	private static final Time ONE_MINUTE = new Time(60, Unit.SECONDS);
	private static final Time ONE_SECOND = new Time(1, Unit.SECONDS);
	
	private FileSystemClient client;
	private MetadataServer metadataServer;
	private Machine jurupoca;
	
	@Before
	public void setup() {
		jurupoca = new Machine("jurupoca", TO_SLEEP_TIMEOUT, TRANSITION_DURATION);
		jurupoca.setIdle(Time.GENESIS, ONE_MINUTE);
		
		Time timeToCoherence = new Time(5 * 60, Unit.SECONDS);
		Time timeToDelete = new Time(5 * 60, Unit.SECONDS);
		Set<DataServer> dataServers = new HashSet<DataServer>();
		dataServers.add(new DataServer(jurupoca));
		boolean wakeOnLan = true;
		DataPlacement dataPlacementAlgorithm = DataPlacement.newDataPlacement(DataPlacement.RANDOM, dataServers);
		metadataServer = new MetadataServer(dataServers, dataPlacementAlgorithm, 0, timeToCoherence, timeToDelete, wakeOnLan);
		client = new FileSystemClient(jurupoca, metadataServer, wakeOnLan);		
	}
	
	@Test
	public void testWritesChangingFileSize() {
		String filePath = "/home/patrick/teste.txt";
		Time zero = Time.GENESIS;
		Time five = new Time(5, Unit.MILLISECONDS);

		Write write = new Write(client, zero, five, filePath, 15, 1024);
		write.process();
		
		write = new Write(client, five, five, filePath, 1024, 2048);
		write.process();
		
		ReplicatedFile file = client.createOrOpen(filePath);

		assertEquals(2048L, file.getSize());
		assertFalse(file.areReplicasConsistent());
	}
	
	/*
	 * If client and data server are in different machine, the target machine is sleeping and client is configured to 
	 * use wakeOnLan...
	 */
	@Test
	public void testWriteIsProperlyReScheduled() { 
		String fullpath = "/home/beefs/arquivo.txt";
		
		// setup the scenario
		ObservableEventSourceMultiplexer eventsMultiplexer = new ObservableEventSourceMultiplexer(new EventSource[0]);
		EventScheduler.setup(Time.GENESIS, Time.THE_FINAL_JUDGMENT, eventsMultiplexer);

		// making jurupoca sleep
		jurupoca.setActive(ONE_MINUTE, ONE_MINUTE);
		jurupoca.setIdle(ONE_MINUTE.times(2), TO_SLEEP_TIMEOUT.plus(ONE_MINUTE));
		EventScheduler.start();
		assertEquals(State.SLEEPING, jurupoca.getState());
		
		Machine awakeMachine = new Machine("cherne", TO_SLEEP_TIMEOUT, TRANSITION_DURATION);
		awakeMachine.setIdle(Time.GENESIS, ONE_MINUTE);
		awakeMachine.setActive(ONE_MINUTE, ONE_MINUTE.times(60));
		FileSystemClient otherClient = new FileSystemClient(awakeMachine, metadataServer, true);
		
		// call write
		Time writeDuration = new Time(500, Unit.MILLISECONDS);
		Time aTimeJurupocaIsSleeping = new Time(17*60 + 4, Unit.SECONDS); // 17 min:4 sec
		Write write = new Write(otherClient, aTimeJurupocaIsSleeping, writeDuration, fullpath, 1024, 1024*1024);
		write.process();
		
		// check if machine's status is waking up
		assertEquals(State.WAKING_UP, jurupoca.getState());
		
		// check if a new Write with the same parameters but different time was scheduled
		Time theTimeJurupocaMustWakeUp = aTimeJurupocaIsSleeping.plus(TRANSITION_DURATION);

		UserIdleness userIdleness = new UserIdleness(jurupoca, theTimeJurupocaMustWakeUp, 
				new Time(18*60, Unit.SECONDS).minus(theTimeJurupocaMustWakeUp), false);
		assertTrue(eventsMultiplexer.contains(userIdleness));
		
		write = new Write(otherClient, theTimeJurupocaMustWakeUp.plus(ONE_SECOND), writeDuration, 
				fullpath, 1024, 1024*1024);
		assertTrue(eventsMultiplexer.contains(write));
		
		EventScheduler.start(); // consumes the UserIdleness scheduled by the call to wakeOnLan
		assertEquals(State.IDLE, jurupoca.getState());
		
		assertEquals(0, client.writesWhileClientSleeping());
	}
	
	/*
	 * If client and data server are in different machine, the target machine is sleeping and client is not configured 
	 * to use wakeOnLan... 
	 */
	@Test
	public void testWriteIsProperlyRegisteredAndIgnored() {
		String fullpath = "/home/beefs/arquivo.txt";
		
		// setup the scenario
		ObservableEventSourceMultiplexer eventsMultiplexer = new ObservableEventSourceMultiplexer(new EventSource[0]);
		EventScheduler.setup(Time.GENESIS, Time.THE_FINAL_JUDGMENT, eventsMultiplexer);

		// making jurupoca sleep
		jurupoca.setActive(ONE_MINUTE, ONE_MINUTE);
		jurupoca.setIdle(ONE_MINUTE.times(2), TO_SLEEP_TIMEOUT.plus(ONE_MINUTE));
		EventScheduler.start();
		assertEquals(State.SLEEPING, jurupoca.getState());
		
		Machine awakeMachine = new Machine("cherne", TO_SLEEP_TIMEOUT, TRANSITION_DURATION);
		awakeMachine.setIdle(Time.GENESIS, ONE_MINUTE);
		awakeMachine.setActive(ONE_MINUTE, ONE_MINUTE.times(60));
		FileSystemClient otherClient = new FileSystemClient(awakeMachine, metadataServer, false);
		
		int queueSizeBefore = eventsMultiplexer.queueSize();
		
		// call write
		Time writeDuration = new Time(500, Unit.MILLISECONDS);
		Time aTimeJurupocaIsSleeping = new Time(17*60 + 4, Unit.SECONDS); // 17 min:4 sec
		Write write = new Write(otherClient, aTimeJurupocaIsSleeping, writeDuration, fullpath, 1024, 1024*1024);
		write.process();
		
		// the status must not change
		assertEquals(State.SLEEPING, jurupoca.getState());
		assertEquals(queueSizeBefore, eventsMultiplexer.queueSize());
		
		assertEquals(1, otherClient.writesWhileDataServerSleeping());
	}
	
	/*
	 * If client machine is sleeping...
	 */
	@Test
	public void testWriteIsProperlyRegisteredAndIgnored1() {
		String fullpath = "/home/beefs/arquivo.txt";
		
		// setup the scenario
		ObservableEventSourceMultiplexer eventsMultiplexer = new ObservableEventSourceMultiplexer(new EventSource[0]);
		EventScheduler.setup(Time.GENESIS, Time.THE_FINAL_JUDGMENT, eventsMultiplexer);
		
		// making jurupoca sleep
		jurupoca.setActive(ONE_MINUTE, ONE_MINUTE);
		jurupoca.setIdle(ONE_MINUTE.times(2), TO_SLEEP_TIMEOUT.plus(ONE_MINUTE));
		EventScheduler.start();
		assertEquals(State.SLEEPING, jurupoca.getState());
		
		int queueSizeBefore = eventsMultiplexer.queueSize();
		
		// call write
		Time writeDuration = new Time(500, Unit.MILLISECONDS);
		Time aTimeJurupocaIsSleeping = new Time(17*60 + 4, Unit.SECONDS); // 17 min:4 sec
		Write write = new Write(client, aTimeJurupocaIsSleeping, writeDuration, fullpath, 1024, 1024*1024);
		write.process();

		// the status must not change
		assertEquals(State.SLEEPING, jurupoca.getState());
		assertEquals(queueSizeBefore, eventsMultiplexer.queueSize());
		
		assertEquals(1, client.writesWhileClientSleeping());
	}

}
