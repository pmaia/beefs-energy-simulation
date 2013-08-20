package simulation.beefs.model;

public class DataServer {

	private long diskUsed = 0L;
	
	private final long diskSize;
	
	private final Machine host;
	
	public DataServer(Machine host, long diskSize) {
		this.host = host;
		this.diskSize = diskSize;
	}
	
	public long diskSize() {
		return diskSize;
	}
	
	public long diskUsed() {
		return diskUsed;
	}
	
	public long freeSpace() {
		return diskSize - diskUsed;
	}
	
	public void useDisk(long bytes) {
		diskUsed += bytes;
	}
	
	public void cleanSpace(long bytes) {
		diskUsed -= bytes;
	}

	public Machine getHost() {
		return host;
	}
}
