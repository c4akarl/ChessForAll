package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class EditUciOptions extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState)
    {

        Log.i(TAG, "onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.editucioptions);
        uciOpts = getIntent().getExtras().getString("uciOpts");
//        uciOptsList = uciOpts.split("\n");
        uciOptsList = getEditableOptions(uciOpts);

        Log.i(TAG, "onCreate(), uciOpts: " + uciOpts);

        //karl ???
        if (uciOptsList != null)
            createViews();

        // file path
        // engine name
    }

    public void createViews()
    {
//        LinearLayout llv = (LinearLayout) findViewById(R.id.eo_content);
        LinearLayout llv = findViewById(R.id.eo_content);
        for (int i = 0; i < uciOptsList.size(); i++) {
            // UCI option can be edited
            LinearLayout llh = new LinearLayout(this);
            llh.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            llh.setOrientation(LinearLayout.HORIZONTAL);
            TextView name = (TextView)getLayoutInflater().inflate(R.layout.eo_string_tv, null);
            EditText editName = (EditText)getLayoutInflater().inflate(R.layout.eo_string_et, null);
            CheckBox checkBox = (CheckBox)getLayoutInflater().inflate(R.layout.eo_check, null);
            Button button = (Button)getLayoutInflater().inflate(R.layout.eo_button, null);
            switch (getType(uciOptsList.get(i))) {
                case CHECK:
                    checkBox.setText(getName(uciOptsList.get(i)));
                    llh.addView(checkBox);
                    llv.addView(llh);
                    break;
                case SPIN:
                    name.setText(getName(uciOptsList.get(i)) + "(" + getMin(uciOptsList.get(i)) + "-"  + getMax(uciOptsList.get(i) + ")     "));
                    editName.setText(getDefault(uciOptsList.get(i)));
                    llh.addView(name);
                    llh.addView(editName);
                    llv.addView(llh);
                    break;
                case COMBO:

                    //karl ListView

                    break;
                case BUTTON:
//                    button.setText(getName(uciOptsList.get(i)));
//                    llh.addView(button);
//                    llv.addView(llh);
                    break;
                case STRING:
                    name.setText(getName(uciOptsList.get(i)) + "     ");
                    editName.setText(getDefault(uciOptsList.get(i)));
                    llh.addView(name);
                    llh.addView(editName);
                    llv.addView(llh);
                    break;
            }
        }
    }

    public String getName(String option)
    {
        String rValue = "";
        String tmp[] = option.split(" ");
        if (option.startsWith("option name")) {
            for (int i = 2; i < tmp.length; i++) {
                if (tmp[i].equals("type"))
                    break;
                if (i == 2)
                    rValue = rValue + tmp[i];
                else
                    rValue = rValue + " " + tmp[i];
            }
        }
        return rValue;
    }

    public Type getType(String option)
    {
        Boolean getValue = false;
        String tmp[] = option.split(" ");
        for (int i = 0; i < tmp.length; i++) {
            if (getValue) {
                if (tmp[i].equals("check")) return Type.CHECK;
                if (tmp[i].equals("spin")) return Type.SPIN;
                if (tmp[i].equals("combo")) return Type.COMBO;
                if (tmp[i].equals("button")) return Type.BUTTON;
                if (tmp[i].equals("string")) return Type.STRING;
            }
            if (tmp[i].equals("type"))
                getValue = true;
        }
        return Type.STRING;
    }

    public String getDefault(String option)
    {
        Boolean getValue = false;
        String tmp[] = option.split(" ");
        for (int i = 0; i < tmp.length; i++) {
            if (getValue)
                return tmp[i];
            if (tmp[i].equals("default"))
                getValue = true;
        }
        return "";
    }

    public String getMin(String option)
    {
        Boolean getValue = false;
        String tmp[] = option.split(" ");
        for (int i = 0; i < tmp.length; i++) {
            if (getValue)
                return tmp[i];
            if (tmp[i].equals("min"))
                getValue = true;
        }
        return "";
    }
    public String getMax(String option)
    {
        Boolean getValue = false;
        String tmp[] = option.split(" ");
        for (int i = 0; i < tmp.length; i++) {
            if (getValue)
                return tmp[i];
            if (tmp[i].equals("max"))
                getValue = true;
        }
        return "";
    }

    ArrayList<String> getEditableOptions(String uciOptions) {
        int cnt = 0;
        ArrayList<String> editList = new ArrayList<>();
        if (uciOptions != null) {
            String[] tmpList = uciOptions.split("\n");
            for (int i = 0; i < tmpList.length; i++) {
                if (isEditableOption(getName(tmpList[i]))) {
                    editList.add(tmpList[i]);
                    cnt++;
                }
            }
        }
        return editList;
    }

    boolean isEditableOption(String name) {
        // uci options, exclusion
        name = name.toLowerCase(Locale.US);
        if (name.startsWith("uci_")) {
            return false;
        } else {
            String[] ignored = { "hash", "ponder", "multipv",
                    "gaviotatbpath", "syzygypath" };
            return !Arrays.asList(ignored).contains(name);
        }
    }

    final String TAG = "EditUciOptions";
    String uciOpts = "";                  // engine uci options
//    String[] uciOptsList;
//    List<String> uciOptsList;
    ArrayList<String> uciOptsList;
    public enum Type {
        CHECK,
        SPIN,
        COMBO,
        BUTTON,
        STRING
    }
    public Type type;

}
