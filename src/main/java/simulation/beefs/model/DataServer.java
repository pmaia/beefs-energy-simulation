package simulation.beefs.model;


public class DataServer {

	private long freeSpace;
	
	private final Machine host;
	
	public DataServer(Machine host, long freeSpace) {
		this.host = host;
		this.freeSpace = freeSpace;
	}
	
	public long freeSpace() {
		return freeSpace;
	}
	
	public void useDisk(long bytes) {
		freeSpace -= bytes;
	}
	
	public void cleanSpace(long bytes) {
		freeSpace += bytes;
	}

	public Machine getHost() {
		return host;
	}
	
}
