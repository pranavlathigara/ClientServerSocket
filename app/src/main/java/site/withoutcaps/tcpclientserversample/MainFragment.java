package site.withoutcaps.tcpclientserversample;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


public class MainFragment extends Fragment {

    private static final String TAG = "MainFragment";
    private static final String BUTTONTXT = "BUTTONTXT";
    private TextView mConsole_txt;
    private Button mStart_btn;


    public static MainFragment newInstance(String buttonTxt) {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        args.putString(BUTTONTXT, buttonTxt);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);
        mConsole_txt = (TextView) v.findViewById(R.id.console_txt);
        mStart_btn = (Button) v.findViewById(R.id.start_btn);
        if (getArguments() != null)
            mStart_btn.setText(getArguments().getString(BUTTONTXT));
        return v;
    }

    public TextView getConsoleTxt() {
        return mConsole_txt;
    }

}

