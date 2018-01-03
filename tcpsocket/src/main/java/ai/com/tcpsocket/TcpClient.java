package ai.com.tcpsocket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;


public class TcpClient {

    private OnMessageReceived mMessageListener;
    private OnConnect mConnectListener;
    private OnDisconnect mDisconnectListener;

    private PrintWriter mOut;
    private BufferedReader mIn;

    private volatile boolean mRun = false;
    private static final int mConnectionTimeout = 1000;
    private static Socket mSocket;


    public void connectInSeperateThread(final String ip, final int port) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                connect(ip, port);
            }
        }).start();
    }

    public void connectInSeperateThread(final String ip, final String port) {
        connectInSeperateThread(ip, Integer.valueOf(port));
    }

    public void connect(String ip, String port) {
        connect(ip, Integer.valueOf(port));
    }

    public void connect(String ip, int port) {
        mRun = true;
        String serverMessage;
        try {
            mSocket = new Socket();
            mSocket.connect(new InetSocketAddress(InetAddress.getByName(ip), port), mConnectionTimeout);
            if (mConnectListener != null)
                mConnectListener.connected(mSocket, ip, port);

            try {
                mIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                mOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream())), true);
                while (mRun) {
                    serverMessage = mIn.readLine();
                    if (serverMessage != null && mMessageListener != null)
                        mMessageListener.messageReceived(serverMessage);
                    if (serverMessage == null)
                        mRun = false;
                }
            } finally {
                mSocket.close();
                if (mDisconnectListener != null)
                    mDisconnectListener.disconnected(ip, port);
            }

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public String getIpFromDns(String address) throws UnknownHostException {
        return InetAddress.getByName(address).getHostAddress();
    }

    public void send(String message) {
        if (mOut != null && !mOut.checkError()) {
            mOut.print(message);
            mOut.flush();
        }
    }

    public void sendLn(String message) {
        if (mOut != null && !mOut.checkError()) {
            mOut.println(message);
            mOut.flush();
        }
    }

    public void stopClient() {
        mRun = false;
        try {
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Boolean isConnected() {
        return mSocket != null ? mSocket.isConnected() : false;
    }


//----------------------------------------[Listeners]----------------------------------------//

    public void setOnMessageReceivedListener(OnMessageReceived listener) {
        mMessageListener = listener;
    }

    public void setOnConnectListener(OnConnect listener) {
        mConnectListener = listener;
    }

    public void setOnDisconnectListener(OnDisconnect listener) {
        mDisconnectListener = listener;
    }


//----------------------------------------[Interfaces]----------------------------------------//

    public interface OnMessageReceived {
        public void messageReceived(String message);
    }

    public interface OnConnect {
        public void connected(Socket socket, String ip, int port);
    }

    public interface OnDisconnect {
        public void disconnected(String ip, int port);
    }
}