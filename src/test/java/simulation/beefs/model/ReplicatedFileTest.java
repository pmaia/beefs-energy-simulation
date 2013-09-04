package simulation.beefs.model;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import manelsim.Time;

import org.junit.Test;

public class ReplicatedFileTest {

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
		ByteArrayOutputStream outContent = new ByteArrayOutputStream();
		System.setOut(new PrintStream(outContent));

		Machine host = new Machine("jurupoca", Time.GENESIS, Time.GENESIS);
		DataServer primary = new DataServer(host, 1024);
		ReplicatedFile rf = new ReplicatedFile("/teste.txt", primary, 0, null);

		rf.write(2048, 2048);

		assertEquals("%primary write failed in jurupoca (1024 bytes not written) - 0", outContent.toString().trim());
	}
}
