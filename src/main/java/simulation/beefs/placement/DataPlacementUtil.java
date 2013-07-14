package simulation.beefs.placement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import simulation.beefs.model.DataServer;


public class DataPlacementUtil {

	public static Set<DataServer> chooseRandomDataServers(Set<DataServer> availableServers, int numberOfWantedServers) {
		
		List<DataServer> availableServersAsList = new ArrayList<DataServer>();
		availableServersAsList.addAll(availableServers);

		int numberOfSelectedDataServers = 
			(availableServers.size() > numberOfWantedServers) ? numberOfWantedServers : availableServers.size();

		Set<DataServer> randomDataServers = new HashSet<DataServer>();

		List<Integer> randomList = random(availableServers.size());

		for (int i = 0; i < numberOfSelectedDataServers; i++) {
			randomDataServers.add(availableServersAsList.get(randomList.get(i)));
		}

		return randomDataServers;
	}
	
	/**
	 * It returns a list containing unsorted integers between 0 (inclusive) and
	 * n (exclusive). The values do not appear more than one time in the list.
	 * 
	 * @param n
	 *            Upper bound (excluded).
	 * @return A list of n integers.
	 */
	private static List<Integer> random(int n) {

		// n is exclusive, so <=
		if ((n <= 0))
			throw new IllegalArgumentException(
					"ceil value must be positive: n <" + n + ">");

		Random random = new Random();

		List<Integer> samples = new ArrayList<Integer>();
		while (samples.size() < n) {
			int randomValue = random.nextInt(n);
			if (!samples.contains(randomValue)) {
				samples.add(randomValue);
			}
		}
		return samples;
	}

}
