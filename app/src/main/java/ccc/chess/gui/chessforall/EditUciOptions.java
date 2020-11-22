package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EditUciOptions extends Activity implements View.OnTouchListener, TextWatcher
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.editucioptions);

        eo_info = findViewById(R.id.eo_info);
        eo_cancel = findViewById(R.id.eo_cancel);
        eo_reset = findViewById(R.id.eo_reset);
        eo_ok = findViewById(R.id.eo_ok);
        uciOpts = getIntent().getExtras().getString("uciOpts");
        uciOptsChanged = getIntent().getExtras().getString("uciOptsChanged");
        uciEngineName = getIntent().getExtras().getString("uciEngineName");
        uciOptsList = getEditableOptions(uciOpts);
        title = findViewById(R.id.title);
        title.setText(uciEngineName);
        title.setBackgroundResource(R.drawable.rectangleyellow);
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
            editName.setBackgroundResource(R.drawable.rectanglegreen);
            CheckBox checkBox = (CheckBox)getLayoutInflater().inflate(R.layout.eo_check, null);
            View spcHeight = getLayoutInflater().inflate(R.layout.eo_spcheight, null);
            switch (getType(uciOptsList.get(i))) {
                case CHECK:
                    checkBox.setText(getName(uciOptsList.get(i)));
                    checkBox.setOnTouchListener(this);
                    if (getDefault(getType(uciOptsList.get(i)), uciOptsList.get(i)).equals("true"))
                        checkBox.setChecked(true);
                    else
                        checkBox.setChecked(false);
                    checkBox.setId(i);
                    checkBox.setTextColor(getResources().getColor(R.color.text_light));
                    viewList.add(checkBox);
                    llh.addView(checkBox);
                    llv.addView(llh);
                    break;
                case SPIN:
                    name.setText(getName(uciOptsList.get(i)) + "(" + getMin(uciOptsList.get(i)) + "-"  + getMax(uciOptsList.get(i)) + ") ");
                    name.setTextColor(getResources().getColor(R.color.text_light));
                    editName.setMinWidth(2000);
                    editName.setBackgroundResource(R.drawable.rectanglegreen);
                    if (getMin(uciOptsList.get(i)).startsWith("-"))
                        editName.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_CLASS_NUMBER);
                    else
                        editName.setInputType(InputType.TYPE_CLASS_NUMBER);
                    editName.setText(getDefault(getType(uciOptsList.get(i)), uciOptsList.get(i)));
                    name.setId(i + NAME_ADD);
                    name.setOnTouchListener(this);
                    editName.setId(i);
                    editName.addTextChangedListener(this);
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
                    name.setTextColor(getResources().getColor(R.color.text_light));
                    editName.setMinWidth(2000);
                    editName.setText(getDefault(getType(uciOptsList.get(i)), uciOptsList.get(i)));
                    editName.setSelection(editName.getText().length());
                    if (getType(uciOptsList.get(i)) == Type.COMBO)
                        editName.setOnTouchListener(this);
                    name.setId(i + NAME_ADD);
                    name.setOnTouchListener(this);
                    editName.setId(i);
                    if (getType(uciOptsList.get(i)) == Type.STRING && getName(uciOptsList.get(i)).toLowerCase().contains("file")) {
                        editName.setFocusable(false);
                        editName.setFocusableInTouchMode(false);
                        editName.setOnTouchListener(this);
                    }
                    else {
                        editName.addTextChangedListener(this);
                    }
                    viewList.add(editName);
                    llv.addView(spcHeight);
                    llh.addView(name);
                    llh.addView(editName);
                    llv.addView(llh);
                    break;
            }
        }

        String[] split = uciOptsChanged.split("\n");
        if (split.length >= 0) {
            for (int i = 0; i < split.length; i++) {
                if (split[i].startsWith("setoption name")) {
                    String[] splitSet = split[i].split(" ");
                    String name = "";
                    String value = "";
                    Boolean isValue = false;
                    for (int j = 2; j < splitSet.length; j++) {
                        if (isValue) {
                            if (value.equals(""))
                                value = splitSet[j];
                            else
                                value = value + " " + splitSet[j];
                        }
                        else {
                            if (splitSet[j].equals("value"))
                                isValue = true;
                            else {
                                if (name.equals(""))
                                    name = splitSet[j];
                                else
                                    name = name + " " + splitSet[j];
                            }
                        }
                    }
                    for (int k = 0; k < uciOptsList.size(); k++) {
                        if (name.equals(getName(uciOptsList.get(k)))) {
                            switch (getType(uciOptsList.get(k))) {
                                case SPIN:
                                case STRING:
                                case COMBO:
                                    TextView tv;
                                    try {
                                        tv = llv.findViewById(k + NAME_ADD);
                                        if (getDefault(getType(uciOptsList.get(k)), uciOptsList.get(k)).equals(value))
                                            tv.setTextColor(getResources().getColor(R.color.text_light));
                                        else
                                            tv.setTextColor(getResources().getColor(R.color.text_white));
                                    }
                                    catch (NullPointerException e) { }

                                    EditText et = (EditText) viewList.get(k);
                                    et.setBackgroundResource(R.drawable.rectanglegreen);
                                    et.setText(value);
                                    et.setSelection(et.getText().length());
                                    break;
                                case CHECK:
                                    CheckBox cb = (CheckBox) viewList.get(k);
                                    if (value.equals("true"))
                                        cb.setChecked(true);
                                    else
                                        cb.setChecked(false);
                                    String def = getDefault(getType(uciOptsList.get(k)), uciOptsList.get(k));
                                    if (!def.equals(value))
                                        cb.setTextColor(getResources().getColor(R.color.text_white));
                                    break;
                                case BUTTON:
                                    break;
                            }
                        }
                    }
                }
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
                    et.setText(getDefault(getType(uciOptsList.get(i)), uciOptsList.get(i)));
                    et.setSelection(et.getText().length());
                    break;
                case CHECK:
                    CheckBox cb = (CheckBox) viewList.get(i);
                    if (getDefault(getType(uciOptsList.get(i)), uciOptsList.get(i)).equals("true"))
                        cb.setChecked(true);
                    else
                        cb.setChecked(false);
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
            case R.id.eo_info:
                showHtml(R.raw.uci_options_info, R.string.uciOptionsInfo);
                break;
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
                long cTime = System.currentTimeMillis();
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(cTime);
                int dayNum =  c.get(Calendar.DAY_OF_WEEK);
                String day = "";
                switch(dayNum)
                {
                    case Calendar.MONDAY:       day = "Monday, "    ; break;
                    case Calendar.TUESDAY:      day = "Tuesday, "   ; break;
                    case Calendar.WEDNESDAY:    day = "Wednesday, " ; break;
                    case Calendar.THURSDAY:     day = "Thursday, "  ; break;
                    case Calendar.FRIDAY:       day = "Friday, "    ; break;
                    case Calendar.SATURDAY:     day = "Saturday, "  ; break;
                    case Calendar.SUNDAY:       day = "Sunday, "    ; break;
                }
                String date =  DateUtils.formatDateTime(this, cTime, DateUtils.FORMAT_SHOW_DATE);
                String time = DateUtils.formatDateTime(this, cTime, DateUtils.FORMAT_SHOW_TIME);
                uciSetOptsList = "; " + day + date + " " + time + "\n";
                for (int i = 0; i < uciOptsList.size(); i++) {
                    String opt = getOption(i);
                    if (!opt.equals(""))
                        uciSetOptsList =  uciSetOptsList + opt + "\n";
                }
                returnIntent.putExtra("uciOptsChanged", uciSetOptsList);
                setResult(RESULT_OK, returnIntent);
                finish();
                break;
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event)
    {

//        Log.i(TAG, "1 onTouch(), id: " + view.getId());

        int id = view.getId();
        if ( event.getAction() == 1) {
            if (id < NAME_ADD) {

//                Log.i(TAG, "2 onTouch(), id: " + id + ", name: " + getName(uciOptsList.get(id)) + ", event.getAction(): " + event.getAction());

                switch (getType(uciOptsList.get(id))) {
                    case BUTTON:
                        EditText etBtn = (EditText) viewList.get(id);
                        if (etBtn.isSelected()) {
                            etBtn.setBackgroundResource(R.drawable.rectanglegreen);
                            etBtn.setSelected(false);
                        } else {
                            etBtn.setBackgroundResource(R.drawable.rectanglepink);
                            etBtn.setSelected(true);
                        }
                        break;
                    case STRING:    // file action
                        getFileFromExternalFilesDir(id, getExternalFilesDir(null) + File.separator + "engines");
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
                            TextView tv;
                            try {
                                tv = llv.findViewById(id + NAME_ADD);
                                if (getDefault(getType(uciOptsList.get(id)), uciOptsList.get(id)).equals(item.getTitle()))
                                    tv.setTextColor(getResources().getColor(R.color.text_light));
                                else
                                    tv.setTextColor(getResources().getColor(R.color.text_white));
                            } catch (NullPointerException e) { }
                            return true;
                        });
                        popup.show();
                        break;
                    case CHECK:
                        CheckBox cb = (CheckBox) viewList.get(id);
                        if (cb.isChecked())
                            cb.setChecked(false);
                        else
                            cb.setChecked(true);
                        String def = getDefault(getType(uciOptsList.get(id)), uciOptsList.get(id));
                        if (def.equals(String.valueOf(cb.isChecked())))
                            cb.setTextColor(getResources().getColor(R.color.text_light));
                        else
                            cb.setTextColor(getResources().getColor(R.color.text_white));

//                    Log.i(TAG, "onTouch(), id: " + id + ", checked: " + cb.isChecked());

                        break;
                }
            }
            else {
                int idEt = id -NAME_ADD;

//                Log.i(TAG, "onTouch(), id: " + id + ", name: " + getName(uciOptsList.get(idEt)) + ", event.getAction(): " + event.getAction());

                TextView tv;
                try {
                    tv = llv.findViewById(id);
                    tv.setTextColor(getResources().getColor(R.color.text_light));
                } catch (NullPointerException e) { }
                EditText et = (EditText) viewList.get(idEt);
                et.setBackgroundResource(R.drawable.rectanglegreen);
                et.setText(getDefault(getType(uciOptsList.get(idEt)), uciOptsList.get(idEt)));
            }
        }

        return true;
    }

    public String getOption(int resId)
    {

//        Log.i(TAG, "getOption(), resId: " + resId + ", view: " + viewList.get(resId) + ", name: " + getName(uciOptsList.get(resId)) + ", type: " + getType(uciOptsList.get(resId)));

        String def = getDefault(getType(uciOptsList.get(resId)), uciOptsList.get(resId));
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

    public void setOption(int resId, String value)
    {
        switch (getType(uciOptsList.get(resId))) {
            case STRING:
                try {
                    EditText et = (EditText) viewList.get(resId);
                    et.setText(value);
                    et.setSelection(et.getText().length());
                    TextView tv;
                    tv = llv.findViewById(resId + NAME_ADD);
                    if (getDefault(getType(uciOptsList.get(resId)), uciOptsList.get(resId)).equals(value))
                        tv.setTextColor(getResources().getColor(R.color.text_light));
                    else
                        tv.setTextColor(getResources().getColor(R.color.text_white));
                }
                catch (NullPointerException e) {Log.i(TAG, "setOption(), error:\n" + e); }
                break;
        }
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

    public String getDefault(Type type, String option)
    {
        Boolean getValue = false;
        String typeString = "";
        String tmp[] = option.split(" ");
        for (int i = 0; i < tmp.length; i++) {
            if (getValue) {
                if (type == Type.STRING) {
                    if (i == tmp.length -1)
                        typeString = typeString + tmp[i];
                    else
                        typeString = typeString + tmp[i] + " ";
                }
                else
                    return tmp[i];
            }
            if (tmp[i].equals("default"))
                getValue = true;
        }
        if (type == Type.STRING)
            return typeString;
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

    public void getFileFromExternalFilesDir(int viewId, String path) {

//        Log.i(TAG, "getFileFromExternalFilesDir(), uci-name: " + getName(uciOptsList.get(viewId)) + ", path: " + path);

        EditText etView = (EditText) viewList.get(viewId);
        File f = new File(path);
        String[] fileA;
        if(f.isDirectory()) {
            fileA = f.list();
            PopupMenu popup = new PopupMenu(EditUciOptions.this, etView);
            for (int i = 0; i < fileA.length; i++)
            {
                popup.getMenu().add(fileA[i]);
                popup.setOnMenuItemClickListener(item -> {
                    String fName = item.getTitle().toString();
                    String fPath = path + File.separator + fName;
                    File fNew = new File(fPath);
                    if (fNew.isDirectory())
                        getFileFromExternalFilesDir(viewId, fPath);
                    else
                        setOption(viewId, fPath);
                    return true;
                });

            }
            popup.show();
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void afterTextChanged(Editable s) {
        EditText et;
        try {et = (EditText) llv.findViewById(getCurrentFocus().getId());}
        catch (NullPointerException e) {return;}
        String value = et.getText().toString();
        TextView tv;
        try {
            tv = llv.findViewById(et.getId() + NAME_ADD);
            if (getDefault(getType(uciOptsList.get(et.getId())), uciOptsList.get(et.getId())).equals(value))
                tv.setTextColor(getResources().getColor(R.color.text_light));
            else
                tv.setTextColor(getResources().getColor(R.color.text_white));
        }
        catch (NullPointerException e) { }

//        Log.i(TAG, "afterTextChanged(), et.id: " + et.getId());

    }

    public void showHtml(int resId, int resTitleId)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String prompt = "";
        try
        {
            InputStream inputStream = getResources().openRawResource(resId);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            prompt = new String(buffer);
            inputStream.close();
        }
        catch (IOException e) {	e.printStackTrace(); }
        WebView wv = new WebView(this);
        builder.setView(wv);
        wv.loadData(prompt, "text/html; charset=UTF-8", null);

        builder.setTitle(getString(resTitleId));
        AlertDialog alert = builder.create();
        alert.show();
    }

    final String TAG = "EditUciOptions";
    final static int FILE_REQUEST_CODE = 1;
    final String SETOPTION_NAME = "setoption name ";
    final String VALUE = " value ";
    final int NAME_ADD = 10000;
    Intent returnIntent = new Intent();
    TextView title;
    LinearLayout llv = null;
    Button eo_info = null;
    Button eo_cancel = null;
    Button eo_reset = null;
    Button eo_ok = null;

    String uciOpts = "";                    // all supported uci options
    String uciOptsChanged = "";             // uci options changed (!= default)
    String uciEngineName = "";              // uci engine name
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
