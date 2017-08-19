import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeSet;

public class SpanningTree {

	static ArrayList<Node> nodes = new ArrayList<>();
	static boolean visited = false; // volatile
	volatile static TreeSet<Integer> treeNodes = new TreeSet<>();
	static volatile int counter = 0;
	static boolean spanningTreeDone = false;
	static Integer parentNode = null;
	static boolean finishedSpanningTree = false;
	static volatile HashMap<Integer, BroadCastMessage> messages = new HashMap<>();
	static volatile long sum = 0;
	static float lambda = 0;
	static volatile int numMsgs = 0;
	volatile static boolean oneTimeExec = true;
	volatile static int msgCount = 1;

	public static void readConfigFile(String fileName) throws IOException {
		File f = new File(fileName);
		Scanner sc = new Scanner(f);
		int k = 1;
		// default 5ms mean time interval and 5 messages will be broadcasted if
		// not configured
		int meanTimeMS = 5;
		int msgs = 5;
		boolean msgDetails = true;
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			if (line.startsWith("#"))
				continue;
			String[] node = line.split("\\s+");
			if (msgDetails && node.length == 2) {
				msgs = Integer.parseInt(node[0]);
				meanTimeMS = Integer.parseInt(node[1]);
				msgDetails = false;
			}
			if (node.length < 3)
				continue;
			ArrayList<Integer> n = new ArrayList<>();
			for (int i = 2; i < node.length; i++)
				n.add(Integer.parseInt(node[i]) - 1);
			nodes.add(new Node(k++, node[0], Integer.parseInt(node[1]), n));
			// portMap.put(Integer.parseInt(node[1]), k++);
		}
		numMsgs = msgs;
		lambda = 1000 / meanTimeMS;

		sc.close();
	}

	/**
	 * to build spanning tree
	 * @param n - server/system number
	 */
	public static void startServer(final int n) throws InterruptedException, ClassNotFoundException {
		//starting node - last node
		if (n == nodes.size() - 1) {
			visited = true;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			for (final Integer neighbor : nodes.get(n).neighbors) {
				(new Thread() {
					@Override
					public void run() {
						sendTo(neighbor, n);
					}
				}).start();
			}
		}
		
		//all other nodes start here
		try {
			ServerSocket serverSock = new ServerSocket(nodes.get(n).portNum);
			while (true) {
				if (counter == nodes.get(n).neighbors.size()) {
					spanningTreeDone = true;
					System.out.println("Parent Node: " + parentNode);
					System.out.println("Tree nodes of " + n);
					for (Integer i : treeNodes)
						System.out.println(i);
					System.out.println("Finished constructing Spanning Tree.");
					break;
				}
				Socket sock = serverSock.accept();

				BufferedReader bf = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				String remotePort = bf.readLine();
				PrintWriter pw = new PrintWriter(sock.getOutputStream());
				counter++;
				if (visited) {
					pw.println("NACK");
					pw.flush();
				} else {
					visited = true;
					pw.println("ACK");
					parentNode = Integer.parseInt(remotePort);
					pw.flush();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					for (final Integer neighbor : nodes.get(n).neighbors) {
						(new Thread() {
							@Override
							public void run() {
								sendTo(neighbor, n);
							}
						}).start();
					}
				}
			}
			Thread.sleep(5000);
			serverSock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (parentNode != null)
			treeNodes.add(parentNode);

		Thread.sleep(10000);
		broadcast(n);

	}

	
	/**
	 * Broadcast and converge cast 
	 * @param n : node/system number
	 */
	public static void broadcast(final int n) throws ClassNotFoundException {

		if (n == nodes.size() - 1) {
			int num = n;
			BroadCastMessage msg = new BroadCastMessage(n, n, Integer.toString(num));
			messages.put(n, msg);
			sum += num;
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			oneTimeExec = false;
			sendMsg(n, msg);

		}

		try {
			ServerSocket ss = new ServerSocket(nodes.get(n).portNum);
			while (true) {
				System.out.println("Sever about to start--");
				
				//System.out.println("Sever started--");
				(new Thread(new Runnable() {
					//ObjectInputStream outStream;
					Socket socket;
					int nodeNum;
					@Override
					public void run() {
						if (oneTimeExec) {
							int num = n;
							BroadCastMessage msg = new BroadCastMessage(n, n, Integer.toString(num));
							messages.put(n, msg);
							sum += num;
							oneTimeExec = false;
							sendMsg(n, msg);
						}
						System.out.println("Server started");
						ObjectInputStream outStream = null;
						Message mss = null;
						try {
							outStream = new ObjectInputStream(socket.getInputStream());
							mss = (Message) outStream.readObject();
						} catch (IOException e2) {
							e2.printStackTrace();
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}

						System.out.println("Msg Src Id: "+mss.srcId);
						if (mss instanceof BroadCastMessage) {
							int connNode = mss.parentNodeId;
							int msgSrcNodeId = mss.srcId;
							String msgVal = ((BroadCastMessage) mss).msg;
							int brdcstVal = Integer.parseInt(msgVal);
							sum += brdcstVal;
							System.out.println("Current total: " + sum);
							try {
								outStream.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
							if (treeNodes.size() == 1) {
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								sendAckToParent(nodeNum, (BroadCastMessage) mss);
							} else {
								BroadCastMessage brdCstMsg = new BroadCastMessage(msgSrcNodeId, connNode, msgVal);
								messages.put(msgSrcNodeId, brdCstMsg);
								sendMsg(nodeNum, brdCstMsg);
							}
						} else if (mss instanceof ReplyMessage) {
							int msgSrcNode = mss.srcId;
							BroadCastMessage m = messages.get(msgSrcNode);
							m.incrementCount();
							if (m.srcId == n && m.count == treeNodes.size()) {
								System.out.println("Message " + msgCount + " Delivered to all the nodes!!");
								System.out.println("Current Sum: " + sum);
								messages.remove(nodeNum);
								numMsgs--;
								if (numMsgs != 0) {
									msgCount++;
									newBroadCast(n);
								}

							} else if (m.srcId != n && m.count == treeNodes.size() - 1) {
								sendAckToParent(nodeNum, m);
								messages.remove(msgSrcNode);
							}
						}
					}

					public Runnable init(Socket soc, int nodeNum) {
						this.socket = soc;
						this.nodeNum = nodeNum;
						return this;
					}
				}.init(ss.accept(),n))).start();
				//s.close();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void sendAckToParent(final int n, final BroadCastMessage m) {
		final int conPortNum = nodes.get(m.parentNodeId).portNum;
		final String hName = nodes.get(m.parentNodeId).hostName;
		//final String hName = "localhost";
		(new Thread() {
			private Socket clientSoc;

			@Override
			public void run() {
				try {
					clientSoc = new Socket(hName, conPortNum);
					ObjectOutputStream oos = new ObjectOutputStream(clientSoc.getOutputStream());
					ReplyMessage rm = new ReplyMessage(m.srcId, n);
					oos.writeObject(rm);
					oos.flush();
					oos.close();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();

	}

	private static void newBroadCast(final int n) {
		Random r = new Random();
		double temp = ((Math.log(1 - r.nextDouble()) / (-lambda))) * 1000;
		int interval = (int) temp;
		System.out.println("--Interval " + interval + "ms--");
		try {
			Thread.sleep(interval);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		int newNum = r.nextInt(100);
		BroadCastMessage msg = new BroadCastMessage(n, n, Integer.toString(newNum));
		messages.put(n, msg);
		sum += newNum;
		System.out.println("Current Sum: " + sum);
		sendMsg(n, msg);
	}

	public static void sendMsg(final int n, final BroadCastMessage m) {

		for (final Integer i : treeNodes) {
			if (i == m.parentNodeId)
				continue;
			// System.out.println("BroadCasting msg to node: " + i);
			(new Thread() {
				private Socket cSock;

				@Override
				public void run() {
					try {
						cSock = new Socket(nodes.get(i).hostName,nodes.get(i).portNum);
						//cSock = new Socket("localhost", nodes.get(i).portNum);
						ObjectOutputStream oos = new ObjectOutputStream(cSock.getOutputStream());
						oos.writeObject(new BroadCastMessage(m.srcId, n, m.msg));
						oos.flush();
						System.out.println("BroadCasting msg to node: " + i);
						oos.close();
					} catch (UnknownHostException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}

	public static void sendTo(int connectTo, int n) {
		try {
			Socket clientSocket = new Socket(nodes.get(connectTo).hostName,nodes.get(connectTo).portNum);
			//Socket clientSocket = new Socket("localhost", nodes.get(connectTo).portNum);
			PrintWriter pw = new PrintWriter(clientSocket.getOutputStream());
			pw.println(Integer.toString(n));
			pw.flush();
			BufferedReader bf = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			String seen = bf.readLine();
			System.out.println(seen);
			if (seen.equals("ACK")) {
				synchronized (SpanningTree.class) {
					treeNodes.add(connectTo);
				}
			}
			clientSocket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException, NumberFormatException, InterruptedException, ClassNotFoundException {
		SpanningTree.readConfigFile(args[1]);
		for (Node n : nodes) {
			System.out.println(n.hostName + "\t" + n.portNum + "\t" + n.neighbors.toString());
		}
		SpanningTree.startServer(Integer.parseInt(args[0]));
	}
}
