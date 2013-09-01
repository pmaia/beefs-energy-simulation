package simulation.beefs.placement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.Set;

import manelsim.Time;

import org.junit.Before;
import org.junit.Test;

import simulation.beefs.model.DataServer;
import simulation.beefs.model.FileSystemClient;
import simulation.beefs.model.Machine;
import simulation.beefs.model.MetadataServer;
import simulation.beefs.model.ReplicatedFile;

public class CoLocatedWithSecondaryRandomTest {
	
	private Machine machine1;
	private Machine machine2;
	private Machine machineForFullDS1;
	private Machine machineForFullDS2;
	private DataServer ds1;
	private DataServer ds2;
	private DataServer fullDS1;
	private DataServer fullDS2;
	
	@Before
	public void setup() {
		machine1 = new Machine("jurupoca", Time.THE_FINAL_JUDGMENT, Time.GENESIS);
		ds1 = new DataServer(machine1, 1024);
		
		machine2 = new Machine("pepino", Time.THE_FINAL_JUDGMENT, Time.GENESIS);
		ds2 = new DataServer(machine2, 1024);
		
		machineForFullDS1 = new Machine("celacanto", Time.THE_FINAL_JUDGMENT, Time.GENESIS);
		fullDS1 = new DataServer(machineForFullDS1, 0);
		
		machineForFullDS2 = new Machine("celacanto", Time.THE_FINAL_JUDGMENT, Time.GENESIS);
		fullDS2 = new DataServer(machineForFullDS2, 0);
	}

	@Test
	public void shouldColocateIfClientDataServerHasFreeSpace() {
		Set<DataServer> dataServers = new HashSet<DataServer>();
		dataServers.add(ds1);
		dataServers.add(ds2);
		
		MetadataServer ms = new MetadataServer(dataServers, null, null, 0, null);
		
		FileSystemClient client =  new FileSystemClient(machine1, ms, false);		
		
		DataPlacement placement = new CoLocatedWithSecondaryRandom(dataServers);
		ReplicatedFile rf = placement.createFile(client, "/home/pmaia/test.txt", 1, 10);

		assertEquals(ds1, rf.primary());
		assertEquals(1, rf.replicas().size());
		assertEquals(ds2, rf.replicas().iterator().next().dataServer());
	}
	
	@Test
	public void shouldNotColocateIfClientDataServerDoesNotHaveFreeSpace() {
		Set<DataServer> dataServers = new HashSet<DataServer>();
		dataServers.add(ds1);
		dataServers.add(ds2);
		dataServers.add(fullDS1);
		
		MetadataServer ms = new MetadataServer(dataServers, null, null, 0, null);
		
		FileSystemClient client =  new FileSystemClient(machineForFullDS1, ms, false);		
		
		DataPlacement placement = new CoLocatedWithSecondaryRandom(dataServers);
		ReplicatedFile rf = placement.createFile(client, "/home/pmaia/test.txt", 1, 10);

		assertNotSame(fullDS1, rf.primary());
		assertEquals(1, rf.replicas().size());
	}
	
	@Test
	public void shouldGiveMeAPrimaryDataServerWithEnoughSpaceForTheFile() {
		Set<DataServer> dataServers = new HashSet<DataServer>();
		dataServers.add(ds1);
		dataServers.add(fullDS1);
		dataServers.add(fullDS2);
		
		MetadataServer ms = new MetadataServer(dataServers, null, null, 0, null);
		
		FileSystemClient client =  new FileSystemClient(machineForFullDS1, ms, false);		
		
		DataPlacement placement = new CoLocatedWithSecondaryRandom(dataServers);
		ReplicatedFile rf = placement.createFile(client, "/home/pmaia/test.txt", 1, 10);

		assertEquals(ds1, rf.primary());
		assertEquals(1, rf.replicas().size());
	}
	
	@Test
	public void shouldGiveMeANullPrimaryDataServerWhenThereIsNoDataServerWithFreeSpace() {
		Set<DataServer> dataServers = new HashSet<DataServer>();
		dataServers.add(fullDS1);
		dataServers.add(fullDS2);
		
		MetadataServer ms = new MetadataServer(dataServers, null, null, 0, null);
		
		FileSystemClient client =  new FileSystemClient(machineForFullDS1, ms, false);		
		
		DataPlacement placement = new CoLocatedWithSecondaryRandom(dataServers);
		ReplicatedFile rf = placement.createFile(client, "/home/pmaia/test.txt", 1, 10);

		assertNull(rf.primary());
		assertEquals(1, rf.replicas().size());
	}
	
	@Test
	public void shouldGiveMeZeroReplicasWhenTheOnlyDataServerWithFreeSpaceIsUsedForPrimary() {
		Set<DataServer> dataServers = new HashSet<DataServer>();
		dataServers.add(fullDS1);
		dataServers.add(ds1);
		
		MetadataServer ms = new MetadataServer(dataServers, null, null, 0, null);
		
		FileSystemClient client =  new FileSystemClient(machineForFullDS1, ms, false);		
		
		DataPlacement placement = new CoLocatedWithSecondaryRandom(dataServers);
		ReplicatedFile rf = placement.createFile(client, "/home/pmaia/test.txt", 1, 10);

		assertEquals(ds1, rf.primary());
		assertEquals(0, rf.replicas().size());
	}
 }
