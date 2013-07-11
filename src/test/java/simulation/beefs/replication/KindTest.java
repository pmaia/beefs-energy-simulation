package simulation.beefs.replication;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import manelsim.Time;
import manelsim.Time.Unit;

import org.junit.Test;

import simulation.beefs.model.DataServer;
import simulation.beefs.model.Machine;
import simulation.beefs.model.Machine.State;
import simulation.beefs.model.ReplicatedFile;

public class KindTest {
	
	@Test
	public void reuse_already_allocated_data_servers_when_they_are_awake() {
		Machine machine1 = createMockMachine(State.ACTIVE, Time.GENESIS);
		Machine machine2 = createMockMachine(State.ACTIVE, Time.GENESIS);
		
		DataServer primary = new DataServer(machine1);
		
		Set<DataServer> replicas = new HashSet<DataServer>();
		replicas.add(new DataServer(machine2));
		
		Kind kind = new Kind(null);
		
		ReplicatedFile file = new ReplicatedFile("/the/file/path", primary, replicas);
		
		ReplicatedFile newFile = kind.updateReplicas(file);
		
		assertEquals(1, newFile.getSecondaries().size());
		assertTrue(replicas.containsAll(newFile.getSecondaries()));
	}

	@Test
	public void allocate_new_servers_if_all_current_seconds_are_sleeping() {
		Machine machine1 = createMockMachine(State.ACTIVE, Time.GENESIS); 
		Machine machine2 = createMockMachine(State.SLEEPING, Time.GENESIS); 
		Machine machine3 = createMockMachine(State.ACTIVE, Time.GENESIS); 
				
		DataServer primary = new DataServer(machine1);
		
		Set<DataServer> originalReplicas = new HashSet<DataServer>();
		originalReplicas.add(new DataServer(machine2));
		
		ReplicatedFile file = new ReplicatedFile("/the/file/path", primary, originalReplicas);
		
		Set<DataServer> availableServers = new HashSet<DataServer>(originalReplicas);
		DataServer ds3 = new DataServer(machine3);
		availableServers.add(ds3);
		
		Kind kind = new Kind(availableServers);
		
		ReplicatedFile newFile = kind.updateReplicas(file);
		
		assertEquals(1, newFile.getSecondaries().size());
		assertTrue(newFile.getSecondaries().contains(ds3));
	}
	
	@Test
	public void allocate_who_is_sleeping_for_longer_if_all_data_servers_are_sleeping() {
		Machine machine1 = createMockMachine(State.ACTIVE, Time.GENESIS); 
		Machine machine2 = createMockMachine(State.SLEEPING, new Time(10, Unit.SECONDS)); 
		Machine machine3 = createMockMachine(State.SLEEPING, Time.GENESIS);
		Machine machine4 = createMockMachine(State.SLEEPING, new Time(11, Unit.SECONDS));
				
		DataServer primary = new DataServer(machine1);
		
		Set<DataServer> originalReplicas = new HashSet<DataServer>();
		originalReplicas.add(new DataServer(machine2));
		
		ReplicatedFile file = new ReplicatedFile("/the/file/path", primary, originalReplicas);
		
		Set<DataServer> availableServers = new HashSet<DataServer>(originalReplicas);
		DataServer ds3 = new DataServer(machine3);
		availableServers.add(ds3);
		availableServers.add(new DataServer(machine4));
		
		Kind kind = new Kind(availableServers);
		
		ReplicatedFile newFile = kind.updateReplicas(file);
		
		assertEquals(1, newFile.getSecondaries().size());
		assertTrue(newFile.getSecondaries().contains(ds3));
	}
	
	@Test
	public void wakeup_primary_if_it_is_sleeping_during_replicas_update() {
		Machine machine1 = createMock(Machine.class);
		expect(machine1.isReachable()).andStubReturn(false);
		machine1.wakeOnLan(Time.GENESIS);
		expectLastCall().once();
		replay(machine1);
		
		Machine machine2 = createMockMachine(State.ACTIVE, new Time(10, Unit.SECONDS)); 
				
		DataServer primary = new DataServer(machine1);
		
		Set<DataServer> originalReplicas = new HashSet<DataServer>();
		originalReplicas.add(new DataServer(machine2));
		
		ReplicatedFile file = new ReplicatedFile("/the/file/path", primary, originalReplicas);
		
		Kind kind = new Kind(null);
		
		kind.updateReplicas(file);
		
		verify(machine1);
	}
	
	private Machine createMockMachine(State state, Time lastTransition) {
		boolean reachable = state.equals(State.IDLE) || state.equals(State.ACTIVE);
		
		Machine machine = createMock(Machine.class);
		expect(machine.getState()).andStubReturn(state);
		expect(machine.lastTransitionTime()).andStubReturn(lastTransition);
		expect(machine.isReachable()).andStubReturn(reachable);
		machine.wakeOnLan(Time.GENESIS);
		replay(machine);
		
		return machine;
	}
}
