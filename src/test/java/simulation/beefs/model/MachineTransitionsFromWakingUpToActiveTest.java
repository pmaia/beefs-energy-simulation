package simulation.beefs.model;

import static org.junit.Assert.assertEquals;
import manelsim.EventScheduler;
import manelsim.EventSource;
import manelsim.Time;
import manelsim.Time.Unit;

import org.junit.Before;
import org.junit.Test;

import simulation.beefs.model.Machine.State;
import simulation.beefs.util.ObservableEventSourceMultiplexer;

/**
 * 
 * @author Patrick Maia
 *
 */
public class MachineTransitionsFromWakingUpToActiveTest {
	
	private final Time TO_SLEEP_TIMEOUT = new Time(15*60, Unit.SECONDS);
	private final Time TRANSITION_DURATION = new Time(2500, Unit.MILLISECONDS);
	private final Time ONE_MINUTE = new Time(60, Unit.SECONDS);
	private final Time ONE_SECOND = new Time(1, Unit.SECONDS);

	private ObservableEventSourceMultiplexer eventsMultiplexer;
	
	private Machine machineWakingUpToActive;
	
	@Before
	public void setup() {
		eventsMultiplexer = new ObservableEventSourceMultiplexer(new EventSource[0]);
		EventScheduler.setup(Time.GENESIS, Time.THE_FINAL_JUDGMENT, eventsMultiplexer);

		machineWakingUpToActive = new Machine("cherne", TO_SLEEP_TIMEOUT, TRANSITION_DURATION);
		machineWakingUpToActive.setIdle(Time.GENESIS, TO_SLEEP_TIMEOUT.plus(ONE_MINUTE));
		machineWakingUpToActive.setSleeping(TO_SLEEP_TIMEOUT, ONE_MINUTE);
		machineWakingUpToActive.setSleeping(TO_SLEEP_TIMEOUT.plus(TRANSITION_DURATION), 
				ONE_MINUTE.minus(TRANSITION_DURATION));
		machineWakingUpToActive.setActive(TO_SLEEP_TIMEOUT.plus(ONE_MINUTE), ONE_MINUTE);
		assertEquals(State.WAKING_UP, machineWakingUpToActive.state());
	}

	/*
	 * It is only possible to wake up to idle when the waking up is motivated by a wakeOnLan and there is still enough
	 * remaining sleeping time (time sufficient to make the transition without an activity event arrive).
	 */
	@Test(expected=IllegalStateException.class)
	public void testTransitionToIdle() {
		machineWakingUpToActive.setIdle(TO_SLEEP_TIMEOUT.plus(ONE_MINUTE.plus(TRANSITION_DURATION)), ONE_MINUTE);
	}
	
	@Test(expected=IllegalStateException.class)
	public void testTransitionToSleep() {
		machineWakingUpToActive.setSleeping(TO_SLEEP_TIMEOUT.plus(ONE_MINUTE.plus(TRANSITION_DURATION)), ONE_MINUTE);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testTransitionToActiveAfterExpectedPeriod() {
		machineWakingUpToActive.setActive(TO_SLEEP_TIMEOUT.plus(ONE_MINUTE.plus(TRANSITION_DURATION.plus(ONE_SECOND))),
				ONE_MINUTE);
	}
	
	@Test
	public void testTransitionToActiveOnExpectedTime() {
		machineWakingUpToActive.setActive(TO_SLEEP_TIMEOUT.plus(ONE_MINUTE.plus(TRANSITION_DURATION)), ONE_MINUTE);
		assertEquals(TRANSITION_DURATION, machineWakingUpToActive.currentDelay());
		assertEquals(State.ACTIVE, machineWakingUpToActive.state());
	}
	
	@Test
	public void testWakeOnLan() {
		int before = eventsMultiplexer.queueSize();
		machineWakingUpToActive.wakeOnLan(TO_SLEEP_TIMEOUT.plus(ONE_MINUTE).plus(ONE_SECOND)); // this must be innocuous
		assertEquals(before, eventsMultiplexer.queueSize());
		assertEquals(State.WAKING_UP, machineWakingUpToActive.state());
	}

}
