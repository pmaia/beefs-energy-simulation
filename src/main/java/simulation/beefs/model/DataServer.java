package simulation.beefs.model;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import manelsim.Time;
import manelsim.TimeInterval;

/**
 * 
 * @author Patrick Maia
 *
 */
public class DataServer {
	
	private final Machine host;

	private TreeSet<TimeInterval> writeIntervals = new TreeSet<TimeInterval>(new TimeIntervalComparator());
	private TreeSet<TimeInterval> readIntervals = new TreeSet<TimeInterval>(new TimeIntervalComparator());
	
	public DataServer(Machine host) {
		this.host = host;
	}

	public Set<TimeInterval> getWriteIntervals() {
		return combineIntervals(writeIntervals.iterator());
	}
	
	public Set<TimeInterval> getReadIntervals() {
		return combineIntervals(readIntervals.iterator());
	}

	public Machine getHost() {
		return host;
	}

	public void reportWrite(Time start, Time duration) {
//		writeIntervals.add(new TimeInterval(start, start.plus(duration)));
	}

	public void reportRead(Time start, Time duration) {
//		readIntervals.add(new TimeInterval(start, start.plus(duration)));
	}
	
	private Set<TimeInterval> combineIntervals(Iterator<TimeInterval> intervals) {
		Set<TimeInterval> resultSet = new HashSet<TimeInterval>();
		
		TimeInterval firstInterval = intervals.hasNext() ? intervals.next() : null;
		while(firstInterval != null && intervals.hasNext()) {
			TimeInterval secondInterval = intervals.next();
			if(firstInterval.overlaps(secondInterval)) {
				firstInterval = firstInterval.merge(secondInterval);
			} else {
				resultSet.add(firstInterval);
				firstInterval = secondInterval;
			}
		}
		if(firstInterval != null) {
			resultSet.add(firstInterval);
		}
		
		return resultSet;
	}
	
	private static class TimeIntervalComparator implements Comparator<TimeInterval> {
		@Override
		public int compare(TimeInterval interval1, TimeInterval interval2) {
			return interval1.begin().compareTo(interval2.begin());
		}
	}
	
}
