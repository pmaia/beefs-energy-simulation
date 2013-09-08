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
 */
public class MachineTransitionsFromActiveTest {

	private final Time TO_SLEEP_TIMEOUT = new Time(15*60, Unit.SECONDS);
	private final Time TRANSITION_DURATION = new Time(2500, Unit.MILLISECONDS);
	private final Time ACTIVITY_DURATION = new Time(5*60, Unit.SECONDS);
	private final Time TWO_SECONDS = new Time(2, Unit.SECONDS);
	
	private Machine machine;

	@Before
	public void setup() {
		machine = new Machine("jurupoca", TO_SLEEP_TIMEOUT, TRANSITION_DURATION);
		machine.setIdle(Time.GENESIS, Time.GENESIS);
		machine.setActive(Time.GENESIS, ACTIVITY_DURATION);
		
		EventSourceMultiplexer eventsMultiplexer = new EventSourceMultiplexer(new EventSource[0]);
		EventScheduler.setup(Time.GENESIS, Time.THE_FINAL_JUDGMENT, eventsMultiplexer);
	}
	
	@Test(expected=IllegalStateException.class)
	public void testTransitionToActive() {
		machine.setActive(ACTIVITY_DURATION, ACTIVITY_DURATION);
	}
	
	@Test(expected=IllegalStateException.class)
	public void testTransitionToSleep() {
		machine.setSleeping(ACTIVITY_DURATION, TWO_SECONDS);
	}
	
	@Test(expected=IllegalStateException.class)
	public void testWakeOnLan() {
		machine.wakeOnLan(Time.GENESIS);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNonContiguousTransitionToIdle() {
		machine.setIdle(ACTIVITY_DURATION.plus(TWO_SECONDS), TWO_SECONDS);
	}
	
	@Test
	public void testTransitionToIdle1() { // idleness duration is less than TO_SLEEP_TIMEOUT
		machine.setIdle(ACTIVITY_DURATION, TO_SLEEP_TIMEOUT.minus(TWO_SECONDS));
		
		assertEquals(State.IDLE, machine.state());
		assertEquals(0, machine.sleepIntervals().size());
		assertEquals(0, machine.transitionIntervals().size());
		assertEquals(1, machine.userActivityIntervals().size());
		assertEquals(2, machine.userIdlenessIntervals().size()); // there is a transition to idle from bootstrap. see setup()
		
		TimeInterval expectedInterval = new TimeInterval(Time.GENESIS, ACTIVITY_DURATION);
		assertTrue(machine.userActivityIntervals().contains(expectedInterval));
		expectedInterval = new TimeInterval(Time.GENESIS, Time.GENESIS);
		assertTrue(machine.userIdlenessIntervals().contains(expectedInterval));
		expectedInterval = new TimeInterval(ACTIVITY_DURATION, 
				ACTIVITY_DURATION.plus(TO_SLEEP_TIMEOUT.minus(TWO_SECONDS)));
		assertTrue(machine.userIdlenessIntervals().contains(expectedInterval));
	}
	
	@Test
	public void testTransitionToIdle2() { //idleness duration is greater than TO_SLEEP_TIMEOUT
		machine.setIdle(ACTIVITY_DURATION, TO_SLEEP_TIMEOUT.plus(TRANSITION_DURATION.plus(TWO_SECONDS)));
		
		assertEquals(State.IDLE, machine.state());
		assertEquals(0, machine.sleepIntervals().size());
		assertEquals(0, machine.transitionIntervals().size());
		assertEquals(1, machine.userActivityIntervals().size());
		assertEquals(2, machine.userIdlenessIntervals().size()); // there is a transition to idle from bootstrap. see setup()
		
		TimeInterval expectedInterval = new TimeInterval(Time.GENESIS, ACTIVITY_DURATION);
		assertTrue(machine.userActivityIntervals().contains(expectedInterval));
		expectedInterval = new TimeInterval(Time.GENESIS, Time.GENESIS);
		assertTrue(machine.userIdlenessIntervals().contains(expectedInterval));
		expectedInterval = new TimeInterval(ACTIVITY_DURATION, 
				ACTIVITY_DURATION.plus(TO_SLEEP_TIMEOUT));
		assertTrue(machine.userIdlenessIntervals().contains(expectedInterval));
		
		EventScheduler.start();
		assertEquals(2, EventScheduler.eventsCount());
		
		assertEquals(State.SLEEPING, machine.state());
		assertEquals(1, machine.sleepIntervals().size());
		assertEquals(1, machine.transitionIntervals().size());
		assertEquals(1, machine.userActivityIntervals().size());
		assertEquals(2, machine.userIdlenessIntervals().size()); // there is a transition to idle from bootstrap. see setup()

		expectedInterval = new TimeInterval(ACTIVITY_DURATION.plus(TO_SLEEP_TIMEOUT).plus(TRANSITION_DURATION), 
				ACTIVITY_DURATION.plus(TO_SLEEP_TIMEOUT).plus(TRANSITION_DURATION).plus(TWO_SECONDS));
		assertTrue(machine.sleepIntervals().contains(expectedInterval));
		expectedInterval = new TimeInterval(ACTIVITY_DURATION.plus(TO_SLEEP_TIMEOUT), 
				ACTIVITY_DURATION.plus(TO_SLEEP_TIMEOUT).plus(TRANSITION_DURATION));
		assertTrue(machine.transitionIntervals().contains(expectedInterval));
		expectedInterval = new TimeInterval(Time.GENESIS, ACTIVITY_DURATION);
		assertTrue(machine.userActivityIntervals().contains(expectedInterval));
		expectedInterval = new TimeInterval(Time.GENESIS, Time.GENESIS);
		assertTrue(machine.userIdlenessIntervals().contains(expectedInterval));
		expectedInterval = new TimeInterval(ACTIVITY_DURATION, ACTIVITY_DURATION.plus(TO_SLEEP_TIMEOUT));
		assertTrue(machine.userIdlenessIntervals().contains(expectedInterval));
	}
	
	@Test
	public void testTransitionToIdle3() {
		/* Idleness duration is slightly greater than TO_SLEEP_TIMEOUT. 
		 * So slightly, that the remaining time after TO_SLEEP_TIMEOUT is less than TRANSITION_DURATION.
		 * This means that the idle period doesn't encompass the transition time. 
		 */
		machine.setIdle(ACTIVITY_DURATION, 
				TO_SLEEP_TIMEOUT.plus(TRANSITION_DURATION.minus(new Time(1, Unit.MICROSECONDS))));
		
		assertEquals(State.IDLE, machine.state());
		assertEquals(0, machine.sleepIntervals().size());
		assertEquals(0, machine.transitionIntervals().size());
		assertEquals(1, machine.userActivityIntervals().size());
		assertEquals(2, machine.userIdlenessIntervals().size()); // there is a transition to idle from bootstrap. see setup()
		
		TimeInterval expectedInterval = new TimeInterval(Time.GENESIS, ACTIVITY_DURATION);
		assertTrue(machine.userActivityIntervals().contains(expectedInterval));
		expectedInterval = new TimeInterval(Time.GENESIS, Time.GENESIS);
		assertTrue(machine.userIdlenessIntervals().contains(expectedInterval));
		expectedInterval = new TimeInterval(ACTIVITY_DURATION, 
				ACTIVITY_DURATION.plus(TO_SLEEP_TIMEOUT));
		assertTrue(machine.userIdlenessIntervals().contains(expectedInterval));
		
		EventScheduler.start();
		assertEquals(2, EventScheduler.eventsCount());
		
		assertEquals(State.SLEEPING, machine.state());
		assertEquals(1, machine.sleepIntervals().size());
		assertEquals(1, machine.transitionIntervals().size());
		assertEquals(1, machine.userActivityIntervals().size());
		assertEquals(2, machine.userIdlenessIntervals().size()); // there is a transition to idle from bootstrap. see setup()

		expectedInterval = new TimeInterval(ACTIVITY_DURATION.plus(TO_SLEEP_TIMEOUT).plus(TRANSITION_DURATION), 
				ACTIVITY_DURATION.plus(TO_SLEEP_TIMEOUT).plus(TRANSITION_DURATION));
		assertTrue(machine.sleepIntervals().contains(expectedInterval));
		expectedInterval = new TimeInterval(ACTIVITY_DURATION.plus(TO_SLEEP_TIMEOUT), 
				ACTIVITY_DURATION.plus(TO_SLEEP_TIMEOUT).plus(TRANSITION_DURATION));
		assertTrue(machine.transitionIntervals().contains(expectedInterval));
		expectedInterval = new TimeInterval(Time.GENESIS, ACTIVITY_DURATION);
		assertTrue(machine.userActivityIntervals().contains(expectedInterval));
		expectedInterval = new TimeInterval(Time.GENESIS, Time.GENESIS);
		assertTrue(machine.userIdlenessIntervals().contains(expectedInterval));
		expectedInterval = new TimeInterval(ACTIVITY_DURATION, ACTIVITY_DURATION.plus(TO_SLEEP_TIMEOUT));
		assertTrue(machine.userIdlenessIntervals().contains(expectedInterval));
	}
}
