package ai.com.tcpsocket;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;




public class TcpServer {
    private OnMessageReceived mMessageListener;
    private OnConnect mConnectListener;
    private OnDisconnect mDisconnectListener;
    private OnServerClose mServerClosedListener;
    private OnServerStart mServerStartListener;

    private ServerSocket serverSocket;
    private short lastClientIndex = 0;
    private Map<Integer, Client> clients = new HashMap<>();
    private Boolean mRun = false;
    private static final String TAG = "TcpServer";

    public void startServer(String port) {
        startServer(Integer.valueOf(port));
    }

    public void startServer(int port) {
        mRun = true;
        Socket socket = null;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
            mRun = false;
        }

        if (mServerStartListener != null)
            mServerStartListener.serverStarted(port);

        while (mRun) {
            try {
                socket = serverSocket.accept();
                Client client = new Client(socket);
                lastClientIndex++;
                clients.put((int) lastClientIndex, client);
                new Thread(client).start();
                client.setIndex(lastClientIndex);
                if (mConnectListener != null)
                    mConnectListener.connected(socket, socket.getLocalAddress(), +socket.getLocalPort(), socket.getLocalSocketAddress(), lastClientIndex);
            } catch (IOException e) {
                mRun = false;
                break;
            }
        }

        if (mServerClosedListener != null)
            mServerClosedListener.serverClosed(port);
    }

    public void closeServer() {
        try {
            mRun = false;
            serverSocket.close();
            kickAll();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void kickAll() {
        for (Client client : clients.values())
            client.kill();
    }

    public void kick(int clientIndex) {
        clients.get(clientIndex).kill();
    }

    public void sendln(int clientIndex, String message) {
        clients.get(clientIndex).getOutput().println(message);
        clients.get(clientIndex).getOutput().flush();
    }

    public void send(int clientIndex, String message) {
        clients.get(clientIndex).getOutput().print(message);
        clients.get(clientIndex).getOutput().flush();
    }

    public void broadcast(String message) {
        for (Client client : clients.values()) {
            client.getOutput().print(message);
            client.getOutput().flush();
        }
    }

    public void broadcastln(String message) {
        for (Client client : clients.values()) {
            client.getOutput().println(message);
            client.getOutput().flush();
        }
    }

    public Boolean isServerRunning() {
        return mRun;
    }

    public Map<Integer, Client> getClients() {
        return clients;
    }

    public int getClientsCount() {
        return clients.size();
    }

//---------------------------------------------[Listeners]----------------------------------------------//

    public void setOnMessageReceivedListener(OnMessageReceived listener) {
        mMessageListener = listener;
    }

    public void setOnConnectListener(OnConnect listener) {
        mConnectListener = listener;
    }

    public void setOnDisconnectListener(OnDisconnect listener) {
        mDisconnectListener = listener;
    }

    public void setOnServerClosedListener(OnServerClose listener) {
        mServerClosedListener = listener;
    }

    public void setOnServerStartListener(OnServerStart listener) {
        mServerStartListener = listener;
    }


//---------------------------------------------[Interfaces]---------------------------------------------//

    public interface OnMessageReceived {
        public void messageReceived(String message, int clientIndex);
    }

    public interface OnConnect {
        public void connected(Socket socket, InetAddress localAddress, int port, SocketAddress localSocketAddress, int clientIndex);
    }

    public interface OnDisconnect {
        public void disconnected(Socket socket, InetAddress localAddress, int port, SocketAddress localSocketAddress, int clientIndex);
    }

    public interface OnServerClose {
        public void serverClosed(int port);
    }

    public interface OnServerStart {
        public void serverStarted(int port);
    }


//--------------------------------------------[Client class]--------------------------------------------//

    public class Client implements Runnable {

        private PrintWriter output;
        private Socket socket;
        private BufferedReader input;
        private int clientIndex;


        public Client(Socket clientSocket) {
            this.socket = clientSocket;
            try {
                this.input = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream())), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        public void run() {
            while (mRun) {
                try {
                    String line = input.readLine();
                    if (mMessageListener != null)
                        if (line == null) {
                            socket.close();
                            clients.remove(clientIndex);
                            if (mDisconnectListener != null)
                                mDisconnectListener.disconnected(socket, socket.getLocalAddress(), +socket.getLocalPort(), socket.getLocalSocketAddress(), clientIndex);
                            break;
                        } else
                            mMessageListener.messageReceived(line, clientIndex);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void kill() {
            try {
                socket.shutdownInput();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                socket.shutdownOutput();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void setIndex(int index) {
            this.clientIndex = index;
        }

        private PrintWriter getOutput() {
            return output;
        }

        public Socket getSocket() {
            return socket;
        }
    }
}



