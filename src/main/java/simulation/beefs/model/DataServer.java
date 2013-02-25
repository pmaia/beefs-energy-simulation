package simulation.beefs.model;

/**
 * 
 * @author Patrick Maia
 *
 */
public class DataServer {
	
	private final Machine host;

	public DataServer(Machine host) {
		this.host = host;
	}

	public Machine getHost() {
		return host;
	}
}
