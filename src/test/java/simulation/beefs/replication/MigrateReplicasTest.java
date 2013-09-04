package simulation.beefs.replication;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import manelsim.Time;
import manelsim.Time.Unit;

import org.junit.Test;

import simulation.beefs.model.DataServer;
import simulation.beefs.model.FileReplica;
import simulation.beefs.model.Machine;
import simulation.beefs.model.Machine.State;
import simulation.beefs.model.ReplicatedFile;

public class MigrateReplicasTest {
	
	private static final long GIGABYTE = 1024L * 1024 * 1024;
	private static final long TERABYTE = GIGABYTE * 1024;
	
	@Test
	public void reuse_already_allocated_data_servers_when_they_are_awake() {
		Machine machine1 = createMockMachine(State.ACTIVE, Time.GENESIS);
		Machine machine2 = createMockMachine(State.ACTIVE, Time.GENESIS);
		
		DataServer primary = new DataServer(machine1, TERABYTE);
		
		Set<FileReplica> replicas = new HashSet<FileReplica>();
		DataServer ds1 = new DataServer(machine2, TERABYTE);
		replicas.add(new FileReplica(ds1, GIGABYTE));
		
		MigrateReplicas kind = new MigrateReplicas(null);
		
		ReplicatedFile file = new ReplicatedFile("/the/file/path", primary, 1, replicas);
		
		kind.updateReplicas(file);
		
		assertEquals(1, file.replicas().size());
		assertEquals(ds1, file.replicas().iterator().next().dataServer());
	}

	@Test
	public void allocate_new_servers_if_all_current_seconds_are_sleeping() {
		Machine machine1 = createMockMachine(State.ACTIVE, Time.GENESIS); 
		Machine machine2 = createMockMachine(State.SLEEPING, Time.GENESIS); 
		Machine machine3 = createMockMachine(State.ACTIVE, Time.GENESIS); 
				
		DataServer primary = new DataServer(machine1, TERABYTE);
		
		Set<FileReplica> originalReplicas = new HashSet<FileReplica>();
		DataServer ds2 = new DataServer(machine2, TERABYTE);
		originalReplicas.add(new FileReplica(ds2, GIGABYTE));
		
		ReplicatedFile file = new ReplicatedFile("/the/file/path", primary, 1, originalReplicas);
		
		Set<DataServer> availableServers = new HashSet<DataServer>();
		availableServers.add(ds2);
		DataServer ds3 = new DataServer(machine3, TERABYTE);
		availableServers.add(ds3);
		
		MigrateReplicas kind = new MigrateReplicas(availableServers);
		
		kind.updateReplicas(file);
		
		assertEquals(1, file.replicas().size());
		assertEquals(ds3, file.replicas().iterator().next().dataServer());
	}
	
	@Test
	public void allocate_who_is_sleeping_for_longer_if_all_data_servers_are_sleeping() {
		Machine machine1 = createMockMachine(State.ACTIVE, Time.GENESIS); 
		Machine machine2 = createMockMachine(State.SLEEPING, new Time(10, Unit.SECONDS)); 
		Machine machine3 = createMockMachine(State.SLEEPING, Time.GENESIS);
		Machine machine4 = createMockMachine(State.SLEEPING, new Time(11, Unit.SECONDS));
				
		DataServer primary = new DataServer(machine1, TERABYTE);
		
		Set<FileReplica> originalReplicas = new HashSet<FileReplica>();
		DataServer ds2 = new DataServer(machine2, TERABYTE);
		originalReplicas.add(new FileReplica(ds2, GIGABYTE));
		
		ReplicatedFile file = new ReplicatedFile("/the/file/path", primary, 1, originalReplicas);
		
		Set<DataServer> availableServers = new HashSet<DataServer>();
		availableServers.add(ds2);
		DataServer ds3 = new DataServer(machine3, TERABYTE);
		availableServers.add(ds3);
		availableServers.add(new DataServer(machine4, TERABYTE));
		
		MigrateReplicas kind = new MigrateReplicas(availableServers);
		
		kind.updateReplicas(file);
		
		assertEquals(1, file.replicas().size());
		assertEquals(ds3, file.replicas().iterator().next().dataServer());
	}
	
	@Test
	public void wakeup_primary_if_it_is_sleeping_during_replicas_update() {
		Machine machine1 = createMock(Machine.class);
		expect(machine1.isReachable()).andStubReturn(false);
		machine1.wakeOnLan(Time.GENESIS);
		expectLastCall().once();
		replay(machine1);
		
		Machine machine2 = createMockMachine(State.ACTIVE, new Time(10, Unit.SECONDS)); 
				
		DataServer primary = new DataServer(machine1, TERABYTE);
		
		Set<FileReplica> originalReplicas = new HashSet<FileReplica>();
		DataServer ds2 = new DataServer(machine2, TERABYTE);
		originalReplicas.add(new FileReplica(ds2, GIGABYTE));
		
		ReplicatedFile file = new ReplicatedFile("/the/file/path", primary, 1, originalReplicas);
		
		MigrateReplicas kind = new MigrateReplicas(null);
		
		kind.updateReplicas(file);
		
		verify(machine1);
	}
	
	private Machine createMockMachine(State state, Time lastTransition) {
		boolean reachable = state.equals(State.IDLE) || state.equals(State.ACTIVE);
		
		Machine machine = createMock(Machine.class);
		expect(machine.state()).andStubReturn(state);
		expect(machine.lastTransitionTime()).andStubReturn(lastTransition);
		expect(machine.isReachable()).andStubReturn(reachable);
		machine.wakeOnLan(Time.GENESIS);
		replay(machine);
		
		return machine;
	}
}
