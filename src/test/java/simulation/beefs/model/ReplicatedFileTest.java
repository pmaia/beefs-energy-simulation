package simulation.beefs.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import manelsim.Time;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ReplicatedFileTest {
	
	private ByteArrayOutputStream outContent;
	private PrintStream originalOut;
	
	@Before
	public void setup() {
		originalOut = System.out;
		outContent = new ByteArrayOutputStream();
		System.setOut(new PrintStream(outContent));
	}
	
	@After
	public void teardown() {
		System.setOut(originalOut);
	}

	@Test
	public void should_write_the_bytes_that_fit_in_disk_in_case_the_disk_does_not_have_enough_space() {
		Machine host = new Machine("jurupoca", Time.GENESIS, Time.GENESIS);
		DataServer primary = new DataServer(host, 1024);
		ReplicatedFile rf = new ReplicatedFile("/teste.txt", primary, 0, null);

		rf.write(2048, 2048);

		assertEquals(0, primary.freeSpace());
	}

	@Test
	public void should_log_the_bytes_that_were_not_written_because_of_full_disk() {
		Machine host = new Machine("jurupoca", Time.GENESIS, Time.GENESIS);
		DataServer primary = new DataServer(host, 1024);
		ReplicatedFile rf = new ReplicatedFile("/teste.txt", primary, 0, null);

		rf.write(2048, 2048);

		assertTrue(outContent.toString().contains("%primary write failed in jurupoca (1024 bytes not written) - 0"));
	}
	
	@Test
	public void should_log_number_of_updated_replicas_on_writes() {
		Machine host = new Machine("jurupoca", Time.GENESIS, Time.GENESIS);
		DataServer primary = new DataServer(host, 1024);
		ReplicatedFile rf = new ReplicatedFile("/teste.txt", primary, 0, null);

		rf.write(2048, 2048);

		assertTrue(outContent.toString().contains("!/teste.txt 0 - 0"));
	}
	
	@Test
	public void should_log_number_of_updated_replicas_on_update_replicas() {
		Machine host = new Machine("jurupoca", Time.GENESIS, Time.GENESIS);
		DataServer primary = new DataServer(host, 1024);
		ReplicatedFile rf = new ReplicatedFile("/teste.txt", primary, 0, null);

		rf.write(2048, 2048);
		
		Set<FileReplica> replicas = new HashSet<FileReplica>();
		replicas.add(new FileReplica(new DataServer(new Machine("a", Time.GENESIS,  Time.GENESIS), 1024), 0));
		rf.updateReplicas(replicas);

		assertTrue(outContent.toString().contains("!/teste.txt 1 - 0"));
	}
	
	@Test
	public void should_log_when_the_replicated_file_is_deleted() {
		Machine host = new Machine("jurupoca", Time.GENESIS, Time.GENESIS);
		DataServer primary = new DataServer(host, 1024);
		ReplicatedFile rf = new ReplicatedFile("/teste.txt", primary, 0, null);

		rf.delete();

		assertTrue(outContent.toString().contains("!/teste.txt deleted - 0"));
	}
}
