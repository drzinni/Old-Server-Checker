
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;

/**
 * 
 * @author Evan, Daniel
 */
public class ServerChecker {
	
	static int PID = 0;
	static int SPACEBUILDID = 0;
	
	

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) throws IOException,
	InterruptedException, Exception {
		Scanner spacebuildz = new Scanner(new FileReader("Garrysmod.txt"));
		
		String spacebuildcode = spacebuildz.nextLine();
		
		byte[] addr1 = new byte[] { (byte) 192, (byte) 168, (byte) 1,
				(byte) 4 };
		int spacebuildport = 27016;
		int killwaittime = 5000;  //Delay to wait while the process dies
		int checktime = 30000; //How often to check server status (1 minute)
		
		int timeoutMs = 12000; // 12 seconds
		
		File file = new File("SRCDS Error Log.txt");
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		
		while (true) {			
			FileWriter output = new FileWriter(file, true);			
			
			//BEGIN GMOD SECTION
			long gmod = testDBConn(addr1, spacebuildport, timeoutMs);
			if (gmod == -1) {// THE SERVER IS DOWN
				System.out.println("THE GMOD SERVER IS DOWN!");				
				Calendar cal = Calendar.getInstance();
				output.write("GMOD\t0\t" + dateFormat.format(cal.getTime()));
				output.write("\n");
				if(SPACEBUILDID != 0)
				{
				stopProcessID(SPACEBUILDID);				
				Thread.sleep(killwaittime);
				}
				
				startProcess("\"C:\\CCS3.0\\orangebox\\srcds.exe\" " + spacebuildcode); //-console -game garrysmod -port 27015 +map gm_flatgrass +maxplayers 16");	
				SPACEBUILDID = PID; //set GMOD ID to be the last returned PID since it was just started
				
				
			} else {
				System.out.println("GMOD SERVER IS UP!");
				//Calendar cal = Calendar.getInstance();
				//output.write("GMOD\t1\t" + dateFormat.format(cal.getTime()));
				//output.write("\n");
			}
			//END GMOD SECTION
			
			
			
			output.close();
			Thread.sleep(checktime);
		}
	}

	public static long testDBConn(byte[] addr1, int port, int timeoutMs) {
		// pass in a byte array with the ipv4 address, the port & the max time
		// out required
		long start = -1; // default check value
		long end = -1; // default check value
		long total = -1; // default for bad connection

		// make an unbound socket
		Socket theSock = new Socket();

		try {
			InetAddress addr = InetAddress.getByAddress(addr1);

			SocketAddress sockaddr = new InetSocketAddress(addr, port);

			// Create the socket with a timeout
			// when a timeout occurs, we will get timout exp.
			// also time our connection this gets very close to the real time
			start = System.currentTimeMillis();
			theSock.connect(sockaddr, timeoutMs);
			end = System.currentTimeMillis();
		} catch (UnknownHostException e) {
			start = -1;
			end = -1;
		} catch (SocketTimeoutException e) {
			start = -1;
			end = -1;
		} catch (IOException e) {
			start = -1;
			end = -1;
		} finally {
			if (theSock != null) {
				try {
					theSock.close();
				} catch (IOException e) {
				}
			}

			if ((start != -1) && (end != -1)) {
				total = end - start;
			}
		}

		return total; // returns -1 if timeout
	}

	static final String CMD_START = "C:\\PsTools\\psexec.exe -d ";
	static final String CMD_STOP = "C:\\PsTools\\pskill.exe ";

	public static int startProcess(String processName) throws Exception {
		return execCmd(CMD_START + processName);
	}

	public static int stopProcessID(int processID) throws Exception {
		return execCmd(CMD_STOP + processID);
	}

	static int execCmd(String cmdLine) throws Exception {
		Process process = Runtime.getRuntime().exec(cmdLine);
		StreamPumper outPumper = new StreamPumper(process.getInputStream(),
				System.out);
		StreamPumper errPumper = new StreamPumper(process.getErrorStream(),
				System.err);

		outPumper.start();
		errPumper.start();
		process.waitFor();
		outPumper.join();
		errPumper.join();
		process.destroy();
		return process.exitValue();
		
		//return 0;
	}

	
	
	static class StreamPumper extends Thread {
		private InputStream is;
		private PrintStream os;

		public StreamPumper(InputStream is, PrintStream os) {
			this.is = is;
			this.os = os;
		}

		public void run() {
			try {
				BufferedReader br = new BufferedReader(
						new InputStreamReader(is));
				String line;

				while ((line = br.readLine()) != null){
					os.println(line);
					if(line.contains("ID")){
						int index = line.lastIndexOf("ID");
						String id = line.substring(index + 3);
						PID = Integer.parseInt(id.split("[.]")[0]);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	

}
