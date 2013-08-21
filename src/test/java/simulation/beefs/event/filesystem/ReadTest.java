package simulation.beefs.event.filesystem;

import static org.junit.Assert.assertEquals;
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
import simulation.beefs.replication.Faithful;
import simulation.beefs.replication.Replicator;
import simulation.beefs.util.ObservableEventSourceMultiplexer;

public class ReadTest {
	
	private static final long TERABYTE = 1024L * 1024 * 1024 * 1024;
	
	private final Time TRANSITION_DURATION = new Time(2500, Unit.MILLISECONDS);
	private final Time TO_SLEEP_TIMEOUT = new Time(15*60, Unit.SECONDS);
	private final Time ONE_MINUTE = new Time(60, Unit.SECONDS);
	private final Time ONE_SECOND = new Time(1, Unit.SECONDS);
	
	private MetadataServer metadataServer;
	private FileSystemClient client;
	private Machine jurupoca;
	
	@Before
	public void setup() {
		jurupoca = new Machine("jurupoca", TO_SLEEP_TIMEOUT, TRANSITION_DURATION);
		jurupoca.setIdle(Time.GENESIS, ONE_MINUTE);
		
		Set<DataServer> dataServers = new HashSet<DataServer>();
		dataServers.add(new DataServer(jurupoca, TERABYTE));
		
		Time timeToCoherence = new Time(5 * 60, Unit.SECONDS);
		DataPlacement dataPlacementAlgorithm = DataPlacement.newDataPlacement(DataPlacement.RANDOM, dataServers);
		Replicator replicator = new Faithful();
		metadataServer = new MetadataServer(dataServers, dataPlacementAlgorithm, replicator, 0, timeToCoherence);
		
		client = new FileSystemClient(jurupoca, metadataServer, true);
	}
	
	@Test
	public void testFileSizeIsTheSameAfterRead() {
		String filePath = "/home/patrick/cruzeiro.txt";
		ReplicatedFile file = client.createOrOpen(filePath);
		file.write(0, 1024);
		
		Read read = new Read(client, Time.GENESIS, new Time(5, Unit.MILLISECONDS), filePath, 10);
		read.process();
		
		assertEquals(1024, file.size());
	}
	
	/*
	 * If client and data server are in different machine, the target machine is sleeping and client is configured to 
	 * use wakeOnLan...
	 */
	@Test
	public void testReadIsProperlyReScheduled() { 
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
		
		// call read
		Time readDuration = new Time(500, Unit.MILLISECONDS);
		Time aTimeJurupocaIsSleeping = new Time(17*60 + 4, Unit.SECONDS); // 17 min:4 sec
		Read read = new Read(otherClient, aTimeJurupocaIsSleeping, readDuration, fullpath, 1024);
		read.process();
		
		// check if machine's status is waking up
		assertEquals(State.WAKING_UP, jurupoca.getState());
		
		// check if a new Read with the same parameters but different time was scheduled
		Time theTimeJurupocaMustWakeUp = aTimeJurupocaIsSleeping.plus(TRANSITION_DURATION);

		UserIdleness userIdleness = new UserIdleness(jurupoca, theTimeJurupocaMustWakeUp, 
				new Time(18*60, Unit.SECONDS).minus(theTimeJurupocaMustWakeUp), false);
		assertTrue(eventsMultiplexer.contains(userIdleness));
		
		read = new Read(otherClient, theTimeJurupocaMustWakeUp.plus(ONE_SECOND), readDuration, fullpath, 1024);
		assertTrue(eventsMultiplexer.contains(read));
		
		EventScheduler.start(); // consumes the UserIdleness scheduled by the call to wakeOnLan
		assertEquals(State.IDLE, jurupoca.getState());
		assertEquals(0, client.readsWhileClientSleeping());
	}
	
	/*
	 * If client and data server are in different machine, the target machine is sleeping and client is not configured 
	 * to use wakeOnLan... 
	 */
	@Test
	public void testReadIsProperlyRegisteredAndIgnored() {
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
		
		// call read
		Time readDuration = new Time(500, Unit.MILLISECONDS);
		Time aTimeJurupocaIsSleeping = new Time(17*60 + 4, Unit.SECONDS); // 17 min:4 sec
		Read read = new Read(otherClient, aTimeJurupocaIsSleeping, readDuration, fullpath, 1024);
		read.process();
		
		// the status must not change
		assertEquals(State.SLEEPING, jurupoca.getState());
		assertEquals(queueSizeBefore, eventsMultiplexer.queueSize());
		
		assertEquals(1, otherClient.readsWhileDataServerSleeping());
	}
	
	/*
	 * If client machine is sleeping...
	 */
	@Test
	public void testReadIsProperlyRegisteredAndIgnored1() {
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
		
		// call read
		Time readDuration = new Time(500, Unit.MILLISECONDS);
		Time aTimeJurupocaIsSleeping = new Time(17*60 + 4, Unit.SECONDS); // 17 min:4 sec
		Read read = new Read(client, aTimeJurupocaIsSleeping, readDuration, fullpath, 1024);
		read.process();

		// the status must not change
		assertEquals(State.SLEEPING, jurupoca.getState());
		assertEquals(queueSizeBefore, eventsMultiplexer.queueSize());
		
		assertEquals(1, client.readsWhileClientSleeping());
	}

}
