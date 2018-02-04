package site.withoutcaps.tcpclientserversample;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import ai.com.tcpsocket.TcpClient;
import ai.com.tcpsocket.TcpServer;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST_INTERNET = 0;
    private static final int CLIENT = 0;
    private static final int SERVER = 1;

    private int mCurrentPage;
    private int mCurrentConnectedTCP = -1;
    private MainFragment mTcpClient_fragment;
    private MainFragment mTcpServer_fragment;

    private TcpClient mTcpClient;
    private TcpServer mTcpServer;

    private Button mClient_btn;
    private Button mServer_btn;

    private EditText mMessage_txt;
    private MenuItem mDisconnect_btn;
    private SharedPreferences mSharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Testing commit

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        final ActionBar ab = getSupportActionBar();
//        ab.setHomeAsUpIndicator(R.drawable.ic_menu);
//        ab.setDisplayHomeAsUpEnabled(true);

        //mDrawer_layout = (DrawerLayout) findViewById(R.id.drawer_layout);
        //NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        ViewPager viewPager = findViewById(R.id.viewpager);
        TabLayout tabLayout = findViewById(R.id.tabs);
        mMessage_txt = findViewById(R.id.message_txt);

        //setupDrawerContent(navigationView);
        setupViewPager(viewPager);
        tabLayout.setupWithViewPager(viewPager);

        askForPremmisions();

        mSharedPref = getPreferences(Context.MODE_PRIVATE);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                mCurrentPage = position;
                if (position == CLIENT) {
                    updateUI(mTcpClient != null ? mTcpClient.isConnected() : false);
                    mDisconnect_btn.setVisible(mTcpClient != null ? mTcpClient.isConnected() : false);
                    mDisconnect_btn.setTitle(getResources().getString(R.string.client_disconnect_btn));
                } else if (position == SERVER) {
                    updateUI(mTcpServer != null ? mTcpServer.isServerRunning() : false);
                    mDisconnect_btn.setVisible(mTcpServer != null ? mTcpServer.isServerRunning() : false);
                    mDisconnect_btn.setTitle(getResources().getString(R.string.server_disconnect_btn));
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        findViewById(R.id.send_btn).setOnClickListener(this);
        new UIListenersThread().execute();
    }

    private void updateUI(boolean state) {
        mMessage_txt.setFocusable(state);
        mMessage_txt.setClickable(state);
        mMessage_txt.setCursorVisible(state);
        mMessage_txt.setFocusableInTouchMode(state);
    }

    public void click(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog, null);
        final EditText ipTxt = v.findViewById(R.id.ip_txt);
        final SharedPreferences.Editor pref_editor = mSharedPref.edit();

        if (mCurrentPage == CLIENT) {
            if (mCurrentConnectedTCP != -1) {
                Toast.makeText(getApplicationContext(), R.string.server_on, Toast.LENGTH_LONG).show();
                return;
            }
            mClient_btn = (Button) view;
            ipTxt.setText(mSharedPref.getString("client_ip", ""));

            builder.setView(v)
                    .setTitle(getResources().getString(R.string.client_dialog_title))
                    .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            String address = ipTxt.getText().toString();
                            pref_editor.putString("client_ip", address);
                            pref_editor.commit();
                            if (address.contains(".") && address.contains(":"))
                                new TcpClientThread().execute(address);
                        }
                    })
                    .setNegativeButton(getResources().getString(R.string.cancel), null);
        } else if (mCurrentPage == SERVER) {
            if (mCurrentConnectedTCP != -1) {
                Toast.makeText(getApplicationContext(), R.string.client_on, Toast.LENGTH_LONG).show();
                return;
            }
            mServer_btn = (Button) view;
            ipTxt.setHint("1024");
            ipTxt.setText(mSharedPref.getString("server_port", ""));

            builder.setView(v)
                    .setTitle(getResources().getString(R.string.server_dialog_title))
                    .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            String port = ipTxt.getText().toString();
                            pref_editor.putString("server_port", port);
                            pref_editor.commit();
                            if (port.length() > 0 && Integer.parseInt(port) >= 1024)
                                new TcpServerThread().execute(port);
                            else
                                Toast.makeText(getApplicationContext(), R.string.low_port, Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton(getResources().getString(R.string.cancel), null);
        }

        builder.create().show();
    }

    private void askForPremmisions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.INTERNET))
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, PERMISSIONS_REQUEST_INTERNET);
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (mTcpClient != null && mTcpClient.isConnected())
            mTcpClient.stopClient();
        if (mTcpServer != null && mTcpServer.isServerRunning())
            mTcpServer.closeServer();
    }

    @Override
    public void onClick(View v) {
        if (mCurrentPage == CLIENT) {
            if (mTcpClient.isConnected()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mTcpClient.sendLn(mMessage_txt.getText().toString());
                    }
                }).start();
                mTcpClient_fragment.getConsoleTxt().append(getString(R.string.client_cons) + " " + mMessage_txt.getText().toString() + System.getProperty("line.separator"));
            }
        } else if (mCurrentPage == SERVER) {
            if (mTcpServer.isServerRunning()) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mTcpServer.broadcastln(mMessage_txt.getText().toString());
                    }
                }).start();
                mTcpServer_fragment.getConsoleTxt().append(getString(R.string.server_cons) + " " + mMessage_txt.getText().toString() + System.getProperty("line.separator"));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mDisconnect_btn = menu.findItem(R.id.action_disconnect);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
//                mDrawer_layout.openDrawer(GravityCompat.START);
                return true;
            case R.id.action_disconnect:
                if (mCurrentPage == CLIENT)
                    mTcpClient.stopClient();
                else if (mCurrentPage == SERVER)
                    mTcpServer.closeServer();

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupViewPager(ViewPager viewPager) {
        Adapter adapter = new Adapter(getSupportFragmentManager());
        mTcpClient_fragment = MainFragment.newInstance(getResources().getString(R.string.connect));
        mTcpServer_fragment = MainFragment.newInstance(getResources().getString(R.string.listen));

        adapter.addFragment(mTcpClient_fragment, getString(R.string.tcp_client));
        adapter.addFragment(mTcpServer_fragment, getString(R.string.tcp_server));

        viewPager.setAdapter(adapter);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        menuItem.setChecked(true);
//                        mDrawer_layout.closeDrawers();
                        return true;
                    }
                });
    }

    //------------------------------------[TCP Listeners Thread]------------------------------------//

    public class UIListenersThread extends AsyncTask<String, String, Void> {
        @Override
        protected Void doInBackground(String... params) {
            mTcpServer = new TcpServer();
            mTcpClient = new TcpClient();
            mTcpServer.setOnServerStartListener(new TcpServer.OnServerStart() {
                @Override
                public void serverStarted(int port) {
                    publishProgress("serverStarted");
                    mCurrentConnectedTCP = SERVER;
                }
            });
            mTcpServer.setOnServerClosedListener(new TcpServer.OnServerClose() {
                @Override
                public void serverClosed(int port) {
                    publishProgress("serverClosed");
                    mCurrentConnectedTCP = -1;
                }
            });
            mTcpServer.setOnConnectListener(new TcpServer.OnConnect() {
                @Override
                public void connected(Socket socket, InetAddress localAddress, int port, SocketAddress localSocketAddress, int clientIndex) {
                    publishProgress(getString(R.string.client_cons) + localAddress + " Connected, Index: " + clientIndex);
                }
            });

            mTcpServer.setOnDisconnectListener(new TcpServer.OnDisconnect() {
                @Override
                public void disconnected(Socket socket, InetAddress localAddress, int port, SocketAddress localSocketAddress, int clientIndex) {
                    publishProgress(getString(R.string.client_cons) + clientIndex + " Disconnected");

                }
            });
            mTcpClient.setOnConnectListener(new TcpClient.OnConnect() {
                @Override
                public void connected(Socket socket, String ip, int port) {
                    publishProgress("Connected to: " + ip + ":" + port);
                    mCurrentConnectedTCP = CLIENT;
                }
            });

            mTcpClient.setOnDisconnectListener(new TcpClient.OnDisconnect() {
                @Override
                public void disconnected(String ip, int port) {
                    publishProgress("Disconnected: " + ip + ":" + port);
                    mCurrentConnectedTCP = -1;
                }
            });
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            switch (values[0]) {
                case "serverStarted":
                    mServer_btn.setVisibility(View.INVISIBLE);
                    mDisconnect_btn.setVisible(true);
                    mTcpServer_fragment.getConsoleTxt().append(Html.fromHtml("<font color=#9E9E9E>Server Started" + "</font><br>"));
                    break;
                case "serverClosed":
                    mServer_btn.setVisibility(View.VISIBLE);
                    mDisconnect_btn.setVisible(false);
                    mTcpServer_fragment.getConsoleTxt().append(Html.fromHtml("<font color=#9E9E9E>Server Closed" + "</font><br>"));
                    break;
            }
            if (values[0].contains(getString(R.string.client_cons)) && values[0].contains(" Disconnected")) {
                mMessage_txt.setFocusableInTouchMode(mTcpServer.getClientsCount() > 0);
                mTcpServer_fragment.getConsoleTxt().append(Html.fromHtml("<font color=#C62828>" + values[0] + "</font><br>"));
            } else if (values[0].contains(getString(R.string.client_cons)) && values[0].contains(" Connected, Index: ")) {
                mMessage_txt.setFocusableInTouchMode(true);
                mTcpServer_fragment.getConsoleTxt().append(Html.fromHtml("<font color=#558B2F>" + values[0] + "</font><br>"));
            } else if (values[0].contains("Disconnected: ")) {
                mClient_btn.setVisibility(View.VISIBLE);
                updateUI(false);
                mDisconnect_btn.setVisible(false);
                mTcpClient_fragment.getConsoleTxt().append(Html.fromHtml("<font color=#C62828>" + values[0] + "</font><br>"));
            } else if (values[0].contains("Connected")) {
                mClient_btn.setVisibility(View.INVISIBLE);
                updateUI(true);
                mDisconnect_btn.setVisible(true);
                mTcpClient_fragment.getConsoleTxt().append(Html.fromHtml("<font color=#558B2F>" + values[0] + "</font><br>"));
            }
        }
    }
    //------------------------------------[TCP Server Thread]------------------------------------//

    public class TcpServerThread extends AsyncTask<String, String, Void> {

        @Override
        protected Void doInBackground(String... port) {
            mTcpServer.setOnMessageReceivedListener(new TcpServer.OnMessageReceived() {
                @Override
                public void messageReceived(String message, int clientIndex) {
                    publishProgress("Client " + clientIndex + ": " + message);
                }
            });

            mTcpServer.startServer(port[0]);
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            mTcpServer_fragment.getConsoleTxt().append(Html.fromHtml("<font color=#283593> " + values[0] + "</font><br>"));
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mServer_btn.setVisibility(View.VISIBLE);
        }
    }


    //------------------------------------[TCP Client Thread]------------------------------------//
    public class TcpClientThread extends AsyncTask<String, String, Void> {

        @Override
        protected Void doInBackground(String... ip) {
            mTcpClient.setOnMessageReceivedListener(new TcpClient.OnMessageReceived() {
                @Override
                public void messageReceived(String message) {
                    publishProgress(getText(R.string.server_cons) + " " + message);
                }
            });
            try {
                String[] address = ip[0].split(":");
                mTcpClient.connect(address[0], address[1]);
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            mTcpClient_fragment.getConsoleTxt().append(Html.fromHtml("<font color=#283593> " + values[0] + "</font><br>"));
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mClient_btn.setVisibility(View.VISIBLE);
        }
    }

    //------------------------------------[Fragment Pager Adapter]------------------------------------//

    static class Adapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragments = new ArrayList<>();
        private final List<String> mFragmentTitles = new ArrayList<>();

        public Adapter(FragmentManager fm) {
            super(fm);
        }

        public void addFragment(Fragment fragment, String title) {
            mFragments.add(fragment);
            mFragmentTitles.add(title);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitles.get(position);
        }
    }
}
