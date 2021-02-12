package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.kalab.chess.enginesupport.ChessEngine;
import com.kalab.chess.enginesupport.ChessEngineResolver;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ComputerMatch extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        u = new Util();
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        fmPrefs = getSharedPreferences("fm", 0);
        userPrefs = getSharedPreferences("user", 0);
        runPrefs = getSharedPreferences("run", 0);
        u.updateFullscreenStatus(this, userPrefs.getBoolean("user_options_gui_StatusBar", false));
        setContentView(R.layout.computermatch);
        fileManagerIntent = new Intent(this, FileManager.class);
        editUciOptions = new Intent(this, EditUciOptions.class);
        fileIO = new FileIO(this);
        currentBase = runPrefs.getString("run_game0_file_base", "");
        if (!currentBase.equals("") & !currentBase.equals("assets/") & !currentBase.equals("url"))	// == sd-card
        {
            currentPath = runPrefs.getString("run_game0_file_path", "");
            currentFile = runPrefs.getString("run_game0_file_name", "");
        }
        else
        {
            currentPath = fmPrefs.getString("fm_extern_save_path", "");
            currentFile = fmPrefs.getString("fm_extern_save_file", "");
        }
        getPrefs();

        title = findViewById(R.id.title);
        u.setTextViewColors(title, "#6b2c2d", "#f1e622");
        cb_engineVsEngine = findViewById(R.id.cb_engineVsEngine);
        cb_engineVsEngine.setChecked(engineVsEngine);
        cb_engineVsEngine.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    String[] txtSplit = MainActivity.OEX_DEFAULT_ENGINES_MATCH.split("\\|");
                    white = userPrefs.getString("user_play_eve_white", txtSplit[0]);
                    black = userPrefs.getString("user_play_eve_black", txtSplit[1]);
                    engineVsEngine = true;
                }
                else
                {
                    white = runPrefs.getString("run_engineProcess", MainActivity.OEX_DEFAULT_ENGINE_SINGLE);
                    black = "";
                    engineVsEngine = false;
                }
                engine_white_name.setText(white);
                engine_black_name.setText(black);
            }
        });
        engine_white_name = findViewById(R.id.engine_white_name);
        engine_white_name.setText(white);
        engine_black_name = findViewById(R.id.engine_black_name);
        engine_black_name.setText(black);
        et_games = findViewById(R.id.et_games);
        et_games.setText(Integer.toString(games));
        tv_result = findViewById(R.id.tv_result);
        tv_result.setText(result);
        et_event = findViewById(R.id.et_event);
        et_event.setText(event);
        et_site = findViewById(R.id.et_site);
        et_site.setText(site);
        cb_currentGame = findViewById(R.id.cb_currentGame);
        cb_currentGame.setChecked(currentGame);
        cb_changeColor = findViewById(R.id.cb_changeColor);
        cb_changeColor.setChecked(changeColor);
        cb_saveGames = findViewById(R.id.cb_saveGames);
        cb_saveGames.setChecked(saveGames);
        et_path = findViewById(R.id.et_path);
        et_path.setText(path);
        et_file = findViewById(R.id.et_file);
        et_file.setText(file);

    }

    public void myClickHandler(View view)
    {	// ClickHandler	(ButtonEvents)
        Intent returnIntent;
        returnIntent = new Intent();
        switch (view.getId())
        {
            case R.id.btn_engine_white:
                engineId = 0;
                currEngine = white;
                showDialog(MENU_SELECT_ENGINE_FROM_OEX);
                break;
            case R.id.btn_engine_black:
                engineId = 1;
                currEngine = black;
                showDialog(MENU_SELECT_ENGINE_FROM_OEX);
                break;
            case R.id.engine_white_name:
                startEditUciOptions(engine_white_name.getText().toString());
                break;
            case R.id.engine_black_name:
                startEditUciOptions(engine_black_name.getText().toString());
                break;
            case R.id.btn_pgn:
            case R.id.et_path:
            case R.id.et_file:
                if (fileIO.isSdk30()) {
                    removeDialog(NO_FILE_ACTIONS_DIALOG);
                    showDialog(NO_FILE_ACTIONS_DIALOG);
                    return;
                }
                fileManagerIntent.putExtra("fileActionCode", 5);
                fileManagerIntent.putExtra("displayActivity", 1);
                startActivityForResult(fileManagerIntent, ENGINE_AUTOPLAY_REQUEST_CODE);		// start FileManager - Activity(with GUI)
                break;
            case R.id.btn_cancel:
                setResult(RESULT_CANCELED, returnIntent);
                finish();
                break;
            case R.id.btn_start_continue:
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
            case ENGINE_AUTOPLAY_REQUEST_CODE:
                if (resultCode == RESULT_OK)
                {
                    et_path.setText(data.getStringExtra("filePath"));
                    et_file.setText(data.getStringExtra("fileName"));
                }
                break;
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
                                case 0:
                                    white = items.get(item);
                                    engine_white_name.setText(white);
                                    break;
                                case 1:
                                    black = items.get(item);
                                    engine_black_name.setText(black);
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
        engineVsEngine = userPrefs.getBoolean("user_play_eve_engineVsEngine", true);
        if (engineVsEngine) {
            String[] txtSplit = MainActivity.OEX_DEFAULT_ENGINES_MATCH.split("\\|");
            white = userPrefs.getString("user_play_eve_white", txtSplit[0]);
            black = userPrefs.getString("user_play_eve_black", txtSplit[1]);
        }
        else
        {
            white = runPrefs.getString("run_engineProcess", MainActivity.OEX_DEFAULT_ENGINE_SINGLE);
            black = "";
        }
        games = userPrefs.getInt("user_play_eve_games", 1);
        round = userPrefs.getInt("user_play_eve_round", 1);
        result = userPrefs.getString("user_play_eve_result", "1(1), 0-0");
        event = userPrefs.getString("user_play_eve_event", "");
        site = userPrefs.getString("user_play_eve_site", "");
        currentGame = userPrefs.getBoolean("user_play_eve_autoCurrentGame", false);
        changeColor = userPrefs.getBoolean("user_play_eve_autoFlipColor", true);
        saveGames = userPrefs.getBoolean("user_play_eve_autoSave", true);
        path = userPrefs.getString("user_play_eve_path", currentPath);
        file = userPrefs.getString("user_play_eve_file", currentFile);
    }

    protected void setPrefs()
    {
        SharedPreferences.Editor ed = userPrefs.edit();
        ed.putInt("user_play_playMod", 3);
        ed.putBoolean("user_play_eve_engineVsEngine", cb_engineVsEngine.isChecked());
        if (engineVsEngine) {
            ed.putString("user_play_eve_white", engine_white_name.getText().toString());
            ed.putString("user_play_eve_black", engine_black_name.getText().toString());
        }
        ed.putInt("user_play_eve_games", Integer.parseInt (et_games.getText().toString()));
        ed.putString("user_play_eve_result", tv_result.getText().toString());
        ed.putBoolean("user_play_eve_autoCurrentGame", cb_currentGame.isChecked());
        ed.putBoolean("user_play_eve_autoFlipColor", cb_changeColor.isChecked());
        ed.putBoolean("user_play_eve_autoSave", cb_saveGames.isChecked());
        ed.putString("user_play_eve_path", et_path.getText().toString());
        ed.putString("user_play_eve_file", et_file.getText().toString());
        ed.apply();

        SharedPreferences.Editor edR = runPrefs.edit();
        if (engineVsEngine) {
            edR.putString("run_engineListMatch", engine_white_name.getText().toString() + "|" + userPrefs.getString("user_play_eve_black", ""));
        }
        else
            edR.putString("run_engineProcess", engine_white_name.getText().toString());
        edR.apply();

    }

    final String TAG = "ComputerMatch";
    final static int ENGINE_AUTOPLAY_REQUEST_CODE = 50;
    final static int EDIT_UCI_OPTIONS = 51;

    final static int NO_FILE_ACTIONS_DIALOG = 193;
    final static int MENU_SELECT_ENGINE_FROM_OEX = 194;
    Util u;
    SharedPreferences fmPrefs;
    SharedPreferences userPrefs;
    SharedPreferences runPrefs;

    Intent fileManagerIntent;
    Intent editUciOptions;

    private static boolean reservedEngineName(String name) {
        return name.endsWith(".ini");
    }

    FileIO fileIO;
    UciEngine engine;

    boolean engineVsEngine = true;
    String white = "";
    String black = "";
    int games = 1;
    int round = 1;
    String result = "";
    String event = "";
    String site = "";
    boolean currentGame = true;
    boolean changeColor = true;
    boolean saveGames = true;
    String path = "";
    String file = "";

    String currentBase = "";
    String currentPath = "";
    String currentFile = "";

    TextView title;
    CheckBox cb_engineVsEngine;
    TextView engine_white_name;
    TextView engine_black_name;
    EditText et_games;
    TextView tv_result;
    EditText et_event;
    EditText et_site;
    CheckBox cb_currentGame;
    CheckBox cb_changeColor;
    CheckBox cb_saveGames;
    TextView et_path;
    TextView et_file;

    private static String engineDir = "c4a/uci";
    int engineId = 0;
    String currEngine = "";

}
