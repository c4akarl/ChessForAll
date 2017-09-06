package ccc.chess.book;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import ccc.chess.gui.chessforall.MainActivity;

public final class C4aBook 
{
	static class BookEntry 
	{
	    Move move;
	    double weight;
	    BookEntry(Move move) {
	        this.move = move;
	        weight = 1;
	    }
	    @Override
	    public String toString() {
	        return TextIO.moveToUCIString(move) + " (" + weight + ")";
	    }
	}
	MainActivity c4aM;
	private Random rndGen = new SecureRandom();
	
	private IOpeningBook externalBook = new NullBook();
	private BookOptions options = null;
	
	private static final C4aBook INSTANCE = new C4aBook();
	public C4aBook(MainActivity cM)
    {
		c4aM = cM;
    }
	public C4aBook getInstance() 
	{ 
		return INSTANCE; 
	}
	private C4aBook() { rndGen.setSeed(System.currentTimeMillis()); }

	public final void setOptions(BookOptions options) 
	{
	    this.options = options;
	    externalBook = new PolyglotBook(c4aM);
	    externalBook.setOptions(options);
	}

	/** Return a random book move for a position, or null if out of book. */
	public final Move getBookMove(Position pos) 
	{
	    if ((options != null) && (pos.fullMoveCounter > options.maxLength))
	        return null;
	    List<BookEntry> bookMoves = getBook().getBookEntries(pos);
	    if (bookMoves == null)
	        return null;
	
	    ArrayList<Move> legalMoves = new MoveGen().pseudoLegalMoves(pos);
	    legalMoves = MoveGen.removeIllegal(pos, legalMoves);
	    double sum = 0;
	    final int nMoves = bookMoves.size();
	    for (int i = 0; i < nMoves; i++) {
	        BookEntry be = bookMoves.get(i);
	        if (!legalMoves.contains(be.move)) {
	            // If an illegal move was found, it means there was a hash collision,
	            // or a corrupt external book file.
	            return null;
	        }
	        sum += scaleWeight(bookMoves.get(i).weight);
	    }
	    if (sum <= 0) {
	        return null;
	    }
	    double rnd = rndGen.nextDouble() * sum;
	    sum = 0;
	    for (int i = 0; i < nMoves; i++) {
	        sum += scaleWeight(bookMoves.get(i).weight);
	        if (rnd < sum)
	            return bookMoves.get(i).move;
	    }
	    return bookMoves.get(nMoves-1).move;
	}
	
	private final double scaleWeight(double w) 
	{
	    if (w <= 0)
	        return 0;
	    if (options == null)
	        return w;
	    return Math.pow(w, Math.exp(-options.random));
	}
	
	final private IOpeningBook getBook() 
	{
	    return externalBook;
	}

	/** Return a string describing all book moves. */
	public final Pair<String,ArrayList<Move>> getAllBookMoves(Position pos) 
	{
	    StringBuilder ret = new StringBuilder();
	    ArrayList<Move> bookMoveList = new ArrayList<Move>();
	    List<BookEntry> bookMoves = getBook().getBookEntries(pos);
	
	    // Check legality
	    if (bookMoves != null) {
	        ArrayList<Move> legalMoves = new MoveGen().pseudoLegalMoves(pos);
	        legalMoves = MoveGen.removeIllegal(pos, legalMoves);
	        for (int i = 0; i < bookMoves.size(); i++) {
	            BookEntry be = bookMoves.get(i);
	            if (!legalMoves.contains(be.move)) {
	                bookMoves = null;
	                break;
	            }
	        }
	    }
	
	    if (bookMoves != null) {
	        Collections.sort(bookMoves, new Comparator<BookEntry>() {
	            public int compare(BookEntry arg0, BookEntry arg1) {
	                double wd = arg1.weight - arg0.weight;
	                if (wd != 0)
	                    return (wd > 0) ? 1 : -1;
	                String str0 = TextIO.moveToUCIString(arg0.move);
	                String str1 = TextIO.moveToUCIString(arg1.move);
	                return str0.compareTo(str1);
	            }});
	        double totalWeight = 0;
	        for (BookEntry be : bookMoves)
	            totalWeight += scaleWeight(be.weight);
	        if (totalWeight <= 0) totalWeight = 1;
	        for (BookEntry be : bookMoves) {
	            Move m = be.move;
	            bookMoveList.add(m);
	            String moveStr = TextIO.moveToString(pos, m, false);
	            ret.append(moveStr);
	            ret.append(':');
	            int percent = (int)Math.round(scaleWeight(be.weight) * 100 / totalWeight);
	            ret.append(percent);
	            ret.append(' ');
	        }
	    }
	    return new Pair<String, ArrayList<Move>>(ret.toString(), bookMoveList);
	}

	/** Creates the book.bin file. */
	public static void main(String[] args) throws IOException 
	{
	    List<Byte> binBook = createBinBook();
	    FileOutputStream out = new FileOutputStream("../src/book.bin");
	    int bookLen = binBook.size();
	    byte[] binBookA = new byte[bookLen];
	    for (int i = 0; i < bookLen; i++)
	        binBookA[i] = binBook.get(i);
	    out.write(binBookA);
	    out.close();
	}
	
	public static List<Byte> createBinBook() 
	{
	    List<Byte> binBook = new ArrayList<Byte>(0);
	    try {
	        InputStream inStream = new Object().getClass().getResourceAsStream("/book.txt");
	        InputStreamReader inFile = new InputStreamReader(inStream);
	        BufferedReader inBuf = new BufferedReader(inFile, 8192);
	        LineNumberReader lnr = new LineNumberReader(inBuf);
	        String line;
	        while ((line = lnr.readLine()) != null) {
	            if (line.startsWith("#") || (line.length() == 0)) {
	                continue;
	            }
	            if (!addBookLine(line, binBook)) {
	                System.out.printf("Book parse error, line:%d\n", lnr.getLineNumber());
	                throw new RuntimeException();
	            }
	//          System.out.printf("no:%d line:%s%n", lnr.getLineNumber(), line);
	        }
	        lnr.close();
	    } catch (ChessParseError ex) {
	        throw new RuntimeException();
	    } catch (IOException ex) {
	        System.out.println("Can't read opening book resource");
	        throw new RuntimeException();
	    }
	    return binBook;
	}
	
	/** Add a sequence of moves, starting from the initial position, to the binary opening book. */
	private static boolean addBookLine(String line, List<Byte> binBook) throws ChessParseError 
	{
	    Position pos = TextIO.readFEN(TextIO.startPosFEN);
	    UndoInfo ui = new UndoInfo();
	    String[] strMoves = line.split(" ");
	    for (String strMove : strMoves) {
	//        System.out.printf("Adding move:%s\n", strMove);
	        int bad = 0;
	        if (strMove.endsWith("?")) {
	            strMove = strMove.substring(0, strMove.length() - 1);
	            bad = 1;
	        }
	        Move m = TextIO.stringToMove(pos, strMove);
	        if (m == null) {
	            return false;
	        }
	        int prom = pieceToProm(m.promoteTo);
	        int val = m.from + (m.to << 6) + (prom << 12) + (bad << 15);
	        binBook.add((byte)(val >> 8));
	        binBook.add((byte)(val & 255));
	        pos.makeMove(m, ui);
	    }
	    binBook.add((byte)0);
	    binBook.add((byte)0);
	    return true;
	}
	
	private static int pieceToProm(int p) 
	{
	    switch (p) {
	    case Piece.WQUEEN: case Piece.BQUEEN:
	        return 1;
	    case Piece.WROOK: case Piece.BROOK:
	        return 2;
	    case Piece.WBISHOP: case Piece.BBISHOP:
	        return 3;
	    case Piece.WKNIGHT: case Piece.BKNIGHT:
	        return 4;
	    default:
	        return 0;
	    }
	}
}

