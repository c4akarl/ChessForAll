package ccc.chess.gui.chessforall;

public interface EngineListener {

    void notifyEngineInitialized(int engineId);

    void notifySearchResult(int engineId, int id, String fen, String bestmove, String ponder);

    void notifyPV(int engineId, int id, String engineMessage, int score, String searchDisplayMoves);

    void notifyStop(int engineId, int id, UciEngine.EngineState engineState, String fen, String bestmove);

    void reportEngineError(int engineId, String errMsg);

}
