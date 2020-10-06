package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class EditUciOptions extends Activity implements View.OnTouchListener
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //karl, title: engine name
        setContentView(R.layout.editucioptions);
        eo_cancel = findViewById(R.id.eo_cancel);
        eo_reset = findViewById(R.id.eo_reset);
        eo_ok = findViewById(R.id.eo_ok);
        uciOpts = getIntent().getExtras().getString("uciOpts");
        uciOptsList = getEditableOptions(uciOpts);
        if (uciOptsList != null)
            createViews();
    }

    public void createViews()
    {
        viewList =  new ArrayList<>();
        llv = findViewById(R.id.eo_content);
        for (int i = 0; i < uciOptsList.size(); i++) {
            LinearLayout llh = new LinearLayout(this);
            llh.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            llh.setOrientation(LinearLayout.HORIZONTAL);
            TextView name = (TextView)getLayoutInflater().inflate(R.layout.eo_string_tv, null);
            EditText editName = (EditText)getLayoutInflater().inflate(R.layout.eo_string_et, null);
            CheckBox checkBox = (CheckBox)getLayoutInflater().inflate(R.layout.eo_check, null);
            ListView listView = (ListView)getLayoutInflater().inflate(R.layout.eo_listview, null);
            View spcHeight = getLayoutInflater().inflate(R.layout.eo_spcheight, null);
            switch (getType(uciOptsList.get(i))) {
                case CHECK:
                    checkBox.setText(getName(uciOptsList.get(i)));
                    if (getDefault(uciOptsList.get(i)).equals("true"))
                        checkBox.setChecked(true);
                    else
                        checkBox.setChecked(true);
                    checkBox.setId(i);
                    viewList.add(checkBox);
                    llh.addView(checkBox);
                    llv.addView(llh);
                    break;
                case SPIN:
                    name.setText(getName(uciOptsList.get(i)) + "(" + getMin(uciOptsList.get(i)) + "-"  + getMax(uciOptsList.get(i)) + "," + getDefault(uciOptsList.get(i)) + ") ");
                    editName.setMinWidth(2000);
                    editName.setBackgroundResource(R.drawable.rectanglegreen);
                    if (getMin(uciOptsList.get(i)).startsWith("-"))
                        editName.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_CLASS_NUMBER);
                    else
                        editName.setInputType(InputType.TYPE_CLASS_NUMBER);
                    editName.setText(getDefault(uciOptsList.get(i)));
                    editName.setId(i);
                    viewList.add(editName);
                    llv.addView(spcHeight);
                    llh.addView(name);
                    llh.addView(editName);
                    llv.addView(llh);
                    break;
                case BUTTON:
                    editName.setMinWidth(2000);
                    editName.setBackgroundResource(R.drawable.rectanglegreen);
                    editName.setGravity(Gravity.CENTER);
                    editName.setText(getName(uciOptsList.get(i)));
                    editName.setOnTouchListener(this);
                    editName.setId(i);
                    viewList.add(editName);
                    llv.addView(spcHeight);
                    llh.addView(editName);
                    llv.addView(llh);
                    break;
                case STRING:
                case COMBO:
                    name.setText(getName(uciOptsList.get(i)) + " ");
                    editName.setMinWidth(2000);
                    editName.setBackgroundResource(R.drawable.rectanglegreen);
                    editName.setText(getDefault(uciOptsList.get(i)));
                    if (getType(uciOptsList.get(i)) == Type.COMBO)
                        editName.setOnTouchListener(this);
                    editName.setId(i);
                    viewList.add(editName);
                    llv.addView(spcHeight);
                    llh.addView(name);
                    llh.addView(editName);
                    llv.addView(llh);
                    break;
            }
        }
    }

    public void resetOptions()
    {
        uciOptsList = getEditableOptions(uciOpts);
        for (int i = 0; i < uciOptsList.size(); i++) {
            switch (getType(uciOptsList.get(i))) {
                case SPIN:
                case STRING:
                case COMBO:
                    EditText et = (EditText) viewList.get(i);
                    et.setBackgroundResource(R.drawable.rectanglegreen);
                    et.setText(getDefault(uciOptsList.get(i)));
                    break;
                case CHECK:
                    CheckBox cb = (CheckBox) viewList.get(i);
                    if (getDefault(uciOptsList.get(i)).equals("true"))
                        cb.setChecked(true);
                    else
                        cb.setChecked(true);
                    break;
                case BUTTON:
                    EditText etBtn = (EditText) viewList.get(i);
                    etBtn.setBackgroundResource(R.drawable.rectanglegreen);
                    etBtn.setSelected(false);
                    break;
            }
        }
    }

    public void myClickHandler(View view)
    {
        switch (view.getId())
        {
            case R.id.eo_cancel:
                setResult(RESULT_CANCELED, returnIntent);
                finish();
                break;
            case R.id.eo_reset:
                uciOptsList = getEditableOptions(uciOpts);
                if (uciOptsList != null)
                    resetOptions();
                break;
            case R.id.eo_ok:

//                Log.i(TAG, "myClickHandler(), uciOptsList.size(): " + uciOptsList.size() + ", viewList.size(): " +  + viewList.size());

                uciSetOptsList = "";
                for (int i = 0; i < uciOptsList.size(); i++) {
                    String opt = getOption(i);
                    if (!opt.equals(""))
                        uciSetOptsList =  uciSetOptsList + opt + "\n";
                }
                returnIntent.putExtra("uciSetOpts", uciSetOptsList);
                setResult(RESULT_OK, returnIntent);
                finish();
                break;
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event)
    {
        int id = view.getId();
        if ( event.getAction() == 1) {

            Log.i(TAG, "onTouch(), id: " + id + ", name: " + getName(uciOptsList.get(id)) + ", event.getAction(): " + event.getAction());

            switch (getType(uciOptsList.get(id))) {
                case BUTTON:
                    EditText etBtn = (EditText) viewList.get(id);
                    if (etBtn.isSelected()) {
                        etBtn.setBackgroundResource(R.drawable.rectanglegreen);
                        etBtn.setSelected(false);
                    }
                    else {
                        etBtn.setBackgroundResource(R.drawable.rectanglepink);
                        etBtn.setSelected(true);
                    }
                    break;
                case COMBO:
                    List<String> lv = getVar(uciOptsList.get(id));
                    EditText etCombo = (EditText) viewList.get(id);
                    PopupMenu popup = new PopupMenu(EditUciOptions.this, view);
                    for (int i = 0; i < lv.size(); i++) {
                        popup.getMenu().add(lv.get(i));
                    }
                    popup.setOnMenuItemClickListener(item -> {
                        etCombo.setText(item.getTitle());
                        return true;
                    });
                    popup.show();
                    break;
            }
        }

        return true;
    }

    public String getOption(int resId)
    {

//        Log.i(TAG, "getOption(), resId: " + resId + ", view: " + viewList.get(resId) + ", name: " + getName(uciOptsList.get(resId)) + ", type: " + getType(uciOptsList.get(resId)));

        String def = getDefault(uciOptsList.get(resId));
        switch (getType(uciOptsList.get(resId))) {
            case CHECK: {
                try {
                    CheckBox cb = (CheckBox) viewList.get(resId);
                    String val = "true";
                    if (!cb.isChecked())
                        val = "false";
                    if (!val.equals(def))
                        return SETOPTION_NAME + getName(uciOptsList.get(resId)) + VALUE + val;
                    else
                        return "";
                }
                catch (NullPointerException e) {Log.i(TAG, "getOption(), error:\n" + e); return "";}
            }
            case SPIN:
            case STRING:
            case COMBO:
                try {
                    EditText et = (EditText) viewList.get(resId);
                    String val = et.getText().toString();
                    if (getType(uciOptsList.get(resId)) == Type.SPIN)
                        val = checkSpinValue(resId, val);
                    if (!val.equals(def))
                        return SETOPTION_NAME + getName(uciOptsList.get(resId)) + VALUE + val;
                    else
                        return "";
                }
                catch (NullPointerException e) {Log.i(TAG, "getOption(), error:\n" + e); return "";}
            case BUTTON:
                try {
                    EditText et = (EditText) viewList.get(resId);
                    if (et.isSelected())
                        return SETOPTION_NAME + getName(uciOptsList.get(resId)) ;
                    else
                        return "";
                }
                catch (NullPointerException e) {Log.i(TAG, "getOption(), error:\n" + e); return "";}
        }
        return "";
    }

    public String checkSpinValue(int id, String val)
    {
        try {
            int v = Integer.parseInt(val);
            int min = Integer.parseInt(getMin(uciOptsList.get(id)));
            int max = Integer.parseInt(getMax(uciOptsList.get(id)));
            if (v < min)
                return getMin(uciOptsList.get(id));
            if (v > max)
                return getMax(uciOptsList.get(id));
            return val;
        }
        catch(NumberFormatException nfe) { return val; }
    }

    // get methods: uci engine options   (e.g.   option name Hash type spin default 16 min 1 max 4096)
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

    public List<String> getVar(String option)
    {
        int x = 0;
        Boolean getValue = false;
        List<String> values = new ArrayList<>();
        String tmp[] = option.split(" ");
        for (int i = 0; i < tmp.length; i++) {
            if (getValue){
                values.add(tmp[i]);
                x++;
                getValue = false;
            }
            if (tmp[i].equals("var"))
                getValue = true;
        }
        return values;
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

    // uci options, exeptions
    boolean isEditableOption(String name) {
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
    final String SETOPTION_NAME = "setoption name ";
    final String VALUE = " value ";
    Intent returnIntent = new Intent();
    LinearLayout llv = null;
    Button eo_cancel = null;
    Button eo_reset = null;
    Button eo_ok = null;

    String uciOpts = "";                    // all supported uci options
    ArrayList<String> uciOptsList;          // UI options (uciOpts - CfA supported options)
    ArrayList<View> viewList;               // Views from uciOptsList (uciOptsList.size() == viewList.size())!
    String uciSetOptsList;                  // set options (returnIntent --> MainActivity)
    public enum Type {
        CHECK,
        SPIN,
        COMBO,
        BUTTON,
        STRING
    }
    public Type type;

}
