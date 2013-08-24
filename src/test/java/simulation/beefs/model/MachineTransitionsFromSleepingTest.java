package simulation.beefs.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import manelsim.EventScheduler;
import manelsim.EventSource;
import manelsim.EventSourceMultiplexer;
import manelsim.Time;
import manelsim.Time.Unit;
import manelsim.TimeInterval;

import org.junit.Before;
import org.junit.Test;

import simulation.beefs.model.Machine.State;

/**
 * 
 * @author Patrick Maia
 *
 */
public class MachineTransitionsFromSleepingTest {
	
	private final Time TO_SLEEP_TIMEOUT = new Time(15*60, Unit.SECONDS);
	private final Time TRANSITION_DURATION = new Time(2500, Unit.MILLISECONDS);
	private final Time TEN_MINUTES = new Time(10*60, Unit.SECONDS);
	
	private Machine machine;
	
	@Before
	public void setup() {
		EventSourceMultiplexer eventsMultiplexer = new EventSourceMultiplexer(new EventSource[0]);
		EventScheduler.setup(Time.GENESIS, Time.THE_FINAL_JUDGMENT, eventsMultiplexer);
		
		machine = new Machine("jurupoca", TO_SLEEP_TIMEOUT, TRANSITION_DURATION);
		machine.setIdle(Time.GENESIS, TO_SLEEP_TIMEOUT.plus(TEN_MINUTES));
		
		EventScheduler.start();
	}
	
	@Test(expected=IllegalStateException.class)
	public void testTransitionToIdle() {
		machine.setIdle(TO_SLEEP_TIMEOUT.plus(TRANSITION_DURATION.times(2)), TEN_MINUTES);
	}
	
	@Test(expected=IllegalStateException.class)
	public void testTransitionToSleep() {
		machine.setSleeping(TO_SLEEP_TIMEOUT.plus(TEN_MINUTES), TEN_MINUTES);
	}
	
	@Test
	public void testTransitionToActive() {
		machine.setActive(TO_SLEEP_TIMEOUT.plus(TEN_MINUTES), TEN_MINUTES);
		
		assertEquals(State.WAKING_UP, machine.state());
		assertEquals(1, machine.sleepIntervals().size());
		assertEquals(2, machine.transitionIntervals().size()); // remember that there was a transition to sleep
		assertEquals(0, machine.userActivityIntervals().size());
		assertEquals(1, machine.userIdlenessIntervals().size());
		
		EventScheduler.start();
		
		assertEquals(3, EventScheduler.processCount()); // IDLE -> SLEEP, TRANSITION -> SLEEP, TRANSITION -> ACTIVE
		
		assertEquals(State.ACTIVE, machine.state());
		assertEquals(1, machine.sleepIntervals().size());
		assertEquals(2, machine.transitionIntervals().size());
		assertEquals(1, machine.userActivityIntervals().size());
		assertEquals(1, machine.userIdlenessIntervals().size());
		
		TimeInterval expectedInterval = new TimeInterval(TO_SLEEP_TIMEOUT.plus(TRANSITION_DURATION), 
				TO_SLEEP_TIMEOUT.plus(TRANSITION_DURATION).plus(TEN_MINUTES.minus(TRANSITION_DURATION)));
		assertTrue(machine.sleepIntervals().contains(expectedInterval));
		expectedInterval = new TimeInterval(TO_SLEEP_TIMEOUT, TO_SLEEP_TIMEOUT.plus(TRANSITION_DURATION));
		assertTrue(machine.transitionIntervals().contains(expectedInterval));
		expectedInterval = new TimeInterval(TO_SLEEP_TIMEOUT.plus(TEN_MINUTES),
				TO_SLEEP_TIMEOUT.plus(TEN_MINUTES).plus(TRANSITION_DURATION));
		assertTrue(machine.transitionIntervals().contains(expectedInterval));
		expectedInterval = new TimeInterval(TO_SLEEP_TIMEOUT.plus(TEN_MINUTES).plus(TRANSITION_DURATION), 
				TO_SLEEP_TIMEOUT.plus(TEN_MINUTES).plus(TRANSITION_DURATION).plus(TEN_MINUTES));
		assertTrue(machine.userActivityIntervals().contains(expectedInterval));
		expectedInterval = new TimeInterval(Time.GENESIS, TO_SLEEP_TIMEOUT);
		assertTrue(machine.userIdlenessIntervals().contains(expectedInterval));
	}
	
	@Test
	public void testWakeOnLan() {
		machine.wakeOnLan(TO_SLEEP_TIMEOUT.plus(TRANSITION_DURATION));
		
		assertEquals(State.WAKING_UP, machine.state());
		assertEquals(1, machine.sleepIntervals().size());
		assertEquals(2, machine.transitionIntervals().size());
		assertEquals(0, machine.userActivityIntervals().size());
		assertEquals(1, machine.userIdlenessIntervals().size());
		
		EventScheduler.start();
		assertEquals(3, EventScheduler.processCount()); //IDLE -> SLEEP, TRANSITION -> SLEEP, SLEEP -> IDLE
		
		assertEquals(State.IDLE, machine.state());
		assertEquals(1, machine.sleepIntervals().size());
		assertEquals(2, machine.transitionIntervals().size());
		assertEquals(0, machine.userActivityIntervals().size());
		assertEquals(2, machine.userIdlenessIntervals().size());
		
		TimeInterval expectedInterval = new TimeInterval(TO_SLEEP_TIMEOUT.plus(TRANSITION_DURATION), 
				TO_SLEEP_TIMEOUT.plus(TRANSITION_DURATION)); // wakeOnLan is fired immediately after the machine sleep 
		assertTrue(machine.sleepIntervals().contains(expectedInterval));
		
		expectedInterval = new TimeInterval(TO_SLEEP_TIMEOUT, TO_SLEEP_TIMEOUT.plus(TRANSITION_DURATION));
		assertTrue(machine.transitionIntervals().contains(expectedInterval));
		expectedInterval = new TimeInterval(TO_SLEEP_TIMEOUT.plus(TRANSITION_DURATION), 
				TO_SLEEP_TIMEOUT.plus(TRANSITION_DURATION).plus(TRANSITION_DURATION));
		assertTrue(machine.transitionIntervals().contains(expectedInterval));

		expectedInterval = new TimeInterval(Time.GENESIS, TO_SLEEP_TIMEOUT);
		assertTrue(machine.userIdlenessIntervals().contains(expectedInterval));
		expectedInterval = new TimeInterval(TO_SLEEP_TIMEOUT.plus(TRANSITION_DURATION.times(2)),
				TO_SLEEP_TIMEOUT.plus(TRANSITION_DURATION.times(2)).plus(TEN_MINUTES.minus(TRANSITION_DURATION.times(2))));
		assertTrue(machine.userIdlenessIntervals().contains(expectedInterval));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNonContiguousTransitionToActive1() { //TimeInterval is after machine's current state interval 
		machine.setActive(TO_SLEEP_TIMEOUT.plus(TEN_MINUTES.times(2)), TEN_MINUTES);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNonContiguousTransitionToActive2() { //TimeInterval is before machine's current state interval 
		machine.setActive(Time.GENESIS, TEN_MINUTES);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNonContiguousTransitionToActive3() { //TimeInterval overlaps machine's current state interval 
		machine.setActive(TO_SLEEP_TIMEOUT.plus(TRANSITION_DURATION), TEN_MINUTES);
	}
}
