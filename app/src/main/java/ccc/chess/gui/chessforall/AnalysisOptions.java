package ccc.chess.gui.chessforall;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.kalab.chess.enginesupport.ChessEngine;
import com.kalab.chess.enginesupport.ChessEngineResolver;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.N)
public class AnalysisOptions extends Activity {

    @SuppressLint("SetTextI18n")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        u = new Util();
        cv = new ColorValues();
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        userPrefs = getSharedPreferences("user", 0);
        runPrefs = getSharedPreferences("run", 0);
        u.updateFullscreenStatus(this, userPrefs.getBoolean("user_options_gui_StatusBar", false));
        setContentView(R.layout.analysisoptions);
        editUciOptions = new Intent(this, EditUciOptions.class);
        fileIO = new FileIO(this);
        getPrefs();

        title = findViewById(R.id.title);
        u.setTextViewColors(title, "#6b2c2d", "#f1e622");

        cb_multipleEngines = findViewById(R.id.cb_multipleEngines);
        cb_multipleEngines.setChecked(multipleEngines);
//        if (multipleEngines)
//            cb_multipleEngines.setText(R.string.multipleEngines);
//        else
//            cb_multipleEngines.setText(runPrefs.getString("run_engineProcess", MainActivity.OEX_DEFAULT_ENGINE_SINGLE));
        cb_multipleEngines.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                cb_multipleEngines.setText(R.string.multipleEngines);
                String[] txtSplit = MainActivity.OEX_DEFAULT_ENGINES_ANALYSIS.split("\\|");
                e1 = userPrefs.getString("user_play_multipleEngines_e1", txtSplit[0]);
                e2 = userPrefs.getString("user_play_multipleEngines_e2", txtSplit[1]);
                e3 = userPrefs.getString("user_play_multipleEngines_e3", "");
                e4 = userPrefs.getString("user_play_multipleEngines_e4", "");
                e1_name.setText(e1);
                e2_name.setText(e2);
                e3_name.setText(e3);
                e4_name.setText(e4);
                btn_e2.setVisibility(TextView.VISIBLE);
                btn_e3.setVisibility(TextView.VISIBLE);
                btn_e4.setVisibility(TextView.VISIBLE);
                multipleEngines = true;
            }
            else
            {
                cb_multipleEngines.setText(runPrefs.getString("run_engineProcess", MainActivity.OEX_DEFAULT_ENGINE_SINGLE));
                e1 = runPrefs.getString("run_engineProcess", MainActivity.OEX_DEFAULT_ENGINE_SINGLE);
                e1_name.setText(e1);
                e2_name.setText("");
                e3_name.setText("");
                e4_name.setText("");
                btn_e2.setVisibility(TextView.GONE);
                btn_e3.setVisibility(TextView.GONE);
                btn_e4.setVisibility(TextView.GONE);
                multipleEngines = false;
            }
        });
        btn_e1 = findViewById(R.id.btn_e1);
        btn_e1.setText(getString(R.string.engine) + "  1");
        btn_e2 = findViewById(R.id.btn_e2);
        btn_e2.setText(getString(R.string.engine) + "  2");
        btn_e3 = findViewById(R.id.btn_e3);
        btn_e3.setText(getString(R.string.engine) + "  3");
        btn_e4 = findViewById(R.id.btn_e4);
        btn_e4.setText(getString(R.string.engine) + "  4");
        e1_name = findViewById(R.id.e1_name);
        e1_name.setTextColor(cv.getTransparentColorInt(ColorValues.COLOR_ARROWS5_27, ALPHA_VALUE));
        e1_name.setText(e1);
        e2_name = findViewById(R.id.e2_name);
        e2_name.setTextColor(cv.getTransparentColorInt(ColorValues.COLOR_ARROWS6_28, ALPHA_VALUE));
        e2_name.setText(e2);
        e3_name = findViewById(R.id.e3_name);
        e3_name.setTextColor(cv.getTransparentColorInt(ColorValues.COLOR_ARROWS7_29, ALPHA_VALUE));
        e3_name.setText(e3);
        e4_name = findViewById(R.id.e4_name);
        e4_name.setTextColor(cv.getTransparentColorInt(ColorValues.COLOR_ARROWS8_30, ALPHA_VALUE));
        e4_name.setText(e4);
        if (multipleEngines) {
            cb_multipleEngines.setText(R.string.multipleEngines);
            btn_e2.setVisibility(TextView.VISIBLE);
            btn_e3.setVisibility(TextView.VISIBLE);
            btn_e4.setVisibility(TextView.VISIBLE);
        }
        else {
            cb_multipleEngines.setText(runPrefs.getString("run_engineProcess", MainActivity.OEX_DEFAULT_ENGINE_SINGLE));
            btn_e2.setVisibility(TextView.GONE);
            btn_e3.setVisibility(TextView.GONE);
            btn_e4.setVisibility(TextView.GONE);
        }

    }

    @SuppressLint("NonConstantResourceId")
    public void myClickHandler(View view)
    {
        Intent returnIntent;
        returnIntent = new Intent();
        switch (view.getId())
        {
            case R.id.btn_e1:
                engineId = 1;
                currEngine = e1;
                removeDialog(MENU_SELECT_ENGINE_FROM_OEX);
                showDialog(MENU_SELECT_ENGINE_FROM_OEX);
                break;
            case R.id.btn_e2:
                if (multipleEngines) {
                    engineId = 2;
                    currEngine = e2;
                    removeDialog(MENU_SELECT_ENGINE_FROM_OEX);
                    showDialog(MENU_SELECT_ENGINE_FROM_OEX);
                }
                break;
            case R.id.btn_e3:
                if (multipleEngines) {
                    engineId = 3;
                    currEngine = e3;
                    removeDialog(MENU_SELECT_ENGINE_FROM_OEX);
                    showDialog(MENU_SELECT_ENGINE_FROM_OEX);
                }
                break;
            case R.id.btn_e4:
                if (multipleEngines) {
                    engineId = 4;
                    currEngine = e4;
                    removeDialog(MENU_SELECT_ENGINE_FROM_OEX);
                    showDialog(MENU_SELECT_ENGINE_FROM_OEX);
                }
                break;
            case R.id.e1_name:
                if (!e1_name.getText().toString().equals(""))
                    startEditUciOptions(e1_name.getText().toString());
                break;
            case R.id.e2_name:
                if (!e2_name.getText().toString().equals(""))
                    startEditUciOptions(e2_name.getText().toString());
                break;
            case R.id.e3_name:
                if (!e3_name.getText().toString().equals(""))
                    startEditUciOptions(e3_name.getText().toString());
                break;
            case R.id.e4_name:
                if (!e4_name.getText().toString().equals(""))
                    startEditUciOptions(e4_name.getText().toString());
                break;
            case R.id.btn_cancel:
                setResult(RESULT_CANCELED, returnIntent);
                finish();
                break;
            case R.id.btn_apply:
                setPrefs();
                setResult(RESULT_OK, returnIntent);
                finish();
                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch(requestCode)
        {
            case EDIT_UCI_OPTIONS:
                if (resultCode == RESULT_OK) {
                    FileIO f = new FileIO(this);;
                    f.dataToFile(f.getUciExternalPath(), engine.uciFileName, data.getStringExtra("uciOptsChanged"), false);
                }
                break;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id)
    {
        if (id == MENU_SELECT_ENGINE_FROM_OEX)
        {
            final ArrayList<String> items = new ArrayList<>();
            if (engineId >= 3)
                items.add(getString(R.string.none));
            XmlResourceParser parser = getResources().getXml(R.xml.enginelist);
            try {
                int eventType = parser.getEventType();
                while (eventType != XmlResourceParser.END_DOCUMENT) {
                    try {
                        if (eventType == XmlResourceParser.START_TAG) {
                            if (parser.getName().equalsIgnoreCase("engine"))
                                items.add(parser.getAttributeValue(null, "name"));
                        }
                        eventType = parser.next();
                    }
                    catch (IOException ignored) {  }
                }
            }
            catch (XmlPullParserException ignored) { }
            ChessEngineResolver resolver = new ChessEngineResolver(this);
            List<ChessEngine> engines = resolver.resolveEngines();
            ArrayList<android.util.Pair<String,String>> oexEngines = new ArrayList<>();
            for (ChessEngine engine : engines) {
                if ((engine.getName() != null) && (engine.getFileName() != null) &&
                        (engine.getPackageName() != null)) {
                    oexEngines.add(new android.util.Pair<>(FileIO.openExchangeFileName(engine),
                            engine.getName()));

//                    Log.i(TAG, "MENU_SELECT_ENGINE_FROM_OEX,  engine.getEnginePath(): " + engine.getEnginePath());

                }
            }
            Collections.sort(oexEngines, (lhs, rhs) -> lhs.second.compareTo(rhs.second));
            for (android.util.Pair<String,String> eng : oexEngines) {
                if (!eng.second.endsWith(".txt"))
                    items.add(eng.second);
            }

            String[] fileNames = FileIO.findFilesInDirectory(engineDir, fname -> !reservedEngineName(fname));
            for (String file : fileNames) {
                if (!file.endsWith(".txt"))
                    items.add(file);
            }
            int defaultItem = 0;
            final int nEngines = items.size();
            for (int i = 0; i < nEngines; i++) {

//				Log.i(TAG, "MENU_SELECT_ENGINE_FROM_OEX, items.get(i): " + items.get(i) + ", ids.get(i): " + ids.get(i));

                if (items.get(i).equals(currEngine)) {
                    defaultItem = i;
                    break;
                }
            }
            if (items.size() > 0)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.menu_enginesettings_select);
                builder.setSingleChoiceItems(items.toArray(new String[0]), defaultItem,
                        (dialog, item) -> {
                            if ((item < 0) || (item >= nEngines))
                                return;
                            dialog.dismiss();
                            switch (engineId) {
                                case 1:
                                    if (items.get(item).equals(e2) || items.get(item).equals(e3) || items.get(item).equals(e4)) {
                                        Toast.makeText(this, getString(R.string.engineExists), Toast.LENGTH_LONG).show();
                                        break;
                                    }
                                    e1 = items.get(item);
                                    e1_name.setText(e1);
                                    break;
                                case 2:
                                    if (items.get(item).equals(e1) || items.get(item).equals(e3) || items.get(item).equals(e4)) {
                                        Toast.makeText(this, getString(R.string.engineExists), Toast.LENGTH_LONG).show();
                                        break;
                                    }
                                    e2 = items.get(item);
                                    e2_name.setText(e2);
                                    break;
                                case 3:
                                    if (items.get(item).equals(e1) || items.get(item).equals(e2) || items.get(item).equals(e4)) {
                                        Toast.makeText(this, getString(R.string.engineExists), Toast.LENGTH_LONG).show();
                                        break;
                                    }
                                    e3 = items.get(item);
                                    if (e3.equals(getString(R.string.none)))
                                        e3 = "";
                                    e3_name.setText(e3);
                                    break;
                                case 4:
                                    if (items.get(item).equals(e1) || items.get(item).equals(e2) || items.get(item).equals(e3)) {
                                        Toast.makeText(this, getString(R.string.engineExists), Toast.LENGTH_LONG).show();
                                        break;
                                    }
                                    e4 = items.get(item);
                                    if (e4.equals(getString(R.string.none)))
                                        e4 = "";
                                    e4_name.setText(e4);
                                    break;
                            }
                        });
                builder.setOnCancelListener(dialog -> { }
                );
                builder.setCancelable(true);
                AlertDialog alert = builder.create();
                return alert;
            }
        }

        return null;

    }

    public void startEditUciOptions(String oexFileName) {

//        Log.i(TAG, "1 startEditUciOptions(), oexFileName: " + oexFileName);

        engine = new UciEngine(this, 0,null, null);
        engine.engineProcess = oexFileName;
        if (engine.startProcess()) {

//            Log.i(TAG, "2 startEditUciOptions(), engine.startProcess() OK !");

            engine.writeLineToProcess("uci");
            engine.engineState = UciEngine.EngineState.READ_OPTIONS;
            engine.processAlive = engine.readUCIOptions();
            if (engine.processAlive && !engine.uciOptions.equals("")) {

//                Log.i(TAG, "3 startEditUciOptions(), engine.startProcess(),  engine.uciOptions:\n" + engine.uciOptions);
//                Log.i(TAG, "4 startEditUciOptions(), engine.startProcess(),  engine.uciFileName: " + engine.uciFileName);
//                Log.i(TAG, "5 startEditUciOptions(), engine.startProcess(),  engine.uciEngineName: " + engine.uciEngineName);

                editUciOptions.putExtra("uciOpts", engine.uciOptions);
                editUciOptions.putExtra("uciOptsChanged", fileIO.getDataFromUciFile(fileIO.getUciExternalPath(), engine.uciFileName));
                editUciOptions.putExtra("uciEngineName", engine.uciEngineName);
                startActivityForResult(editUciOptions, EDIT_UCI_OPTIONS);
                engine.shutDown();
            }
        }
    }

    protected void getPrefs()
    {
        e1=e2=e3=e4="";
        multipleEngines = userPrefs.getBoolean("user_play_multipleEngines", true);
        String[] txtSplit = MainActivity.OEX_DEFAULT_ENGINES_ANALYSIS.split("\\|");
        if (multipleEngines) {
            e1 = userPrefs.getString("user_play_multipleEngines_e1", txtSplit[0]);
            e2 = userPrefs.getString("user_play_multipleEngines_e2", txtSplit[1]);
            e3 = userPrefs.getString("user_play_multipleEngines_e3", "");
            e4 = userPrefs.getString("user_play_multipleEngines_e4", "");
        }
        else
            e1 = runPrefs.getString("run_engineProcess", MainActivity.OEX_DEFAULT_ENGINE_SINGLE);
    }

    protected void setPrefs()
    {
        SharedPreferences.Editor ed = userPrefs.edit();
        ed.putInt("user_play_playMod", 4);
        ed.putBoolean("user_play_multipleEngines", cb_multipleEngines.isChecked());
        if (multipleEngines) {
            ed.putString("user_play_multipleEngines_e1", e1_name.getText().toString());
            ed.putString("user_play_multipleEngines_e2", e2_name.getText().toString());
            ed.putString("user_play_multipleEngines_e3", e3_name.getText().toString());
            ed.putString("user_play_multipleEngines_e4", e4_name.getText().toString());
        }
        ed.apply();

        SharedPreferences.Editor edR = runPrefs.edit();
        if (multipleEngines) {
            String listMatch = "";
            String[] txtSplit = MainActivity.OEX_DEFAULT_ENGINES_ANALYSIS.split("\\|");
            if (!userPrefs.getString("user_play_multipleEngines_e1", txtSplit[0]).equals(""))
                listMatch = listMatch + userPrefs.getString("user_play_multipleEngines_e1", txtSplit[0]) + "|";
            if (!userPrefs.getString("user_play_multipleEngines_e2", txtSplit[1]).equals(""))
                listMatch = listMatch + userPrefs.getString("user_play_multipleEngines_e2", txtSplit[1]) + "|";
            if (!userPrefs.getString("user_play_multipleEngines_e3", "").equals(""))
                listMatch = listMatch + userPrefs.getString("user_play_multipleEngines_e3", "") + "|";
            if (!userPrefs.getString("user_play_multipleEngines_e4", "").equals(""))
                listMatch = listMatch + userPrefs.getString("user_play_multipleEngines_e4", "");
            edR.putString("run_engineListAnalysis", listMatch);
        }
        else
            edR.putString("run_engineProcess", e1_name.getText().toString());
        edR.apply();

    }

    final String TAG = "AnalysisOptions";
    final static int EDIT_UCI_OPTIONS = 51;
//    final static int NO_FILE_ACTIONS_DIALOG = 193;
    final static int MENU_SELECT_ENGINE_FROM_OEX = 194;
    final static String ALPHA_VALUE = "ff";

    Util u;
    ColorValues cv;
    SharedPreferences userPrefs;
    SharedPreferences runPrefs;

    Intent editUciOptions;

    private static boolean reservedEngineName(String name) {
        return name.endsWith(".ini");
    }

    FileIO fileIO;
    UciEngine engine;

    boolean multipleEngines = true;
    String e1 = "";
    String e2 = "";
    String e3 = "";
    String e4 = "";

    TextView title;
    CheckBox cb_multipleEngines;
    TextView btn_e1;
    TextView btn_e2;
    TextView btn_e3;
    TextView btn_e4;
    TextView e1_name;
    TextView e2_name;
    TextView e3_name;
    TextView e4_name;

    private static String engineDir = "c4a/uci";
    int engineId = 0;
    String currEngine = "";

}