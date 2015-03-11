package ccc.chess.book;

/** Settings controlling opening book usage */
public class BookOptions 
{
    public BookOptions() { }
    public BookOptions(BookOptions other) 
    {
        filename = other.filename;
        maxLength = other.maxLength;
        preferMainLines = other.preferMainLines;
        tournamentMode = other.tournamentMode;
        random = other.random;
    }
    @Override
    public boolean equals(Object o) 
    {
        if ((o == null) || (o.getClass() != this.getClass()))
            return false;
        BookOptions other = (BookOptions)o;

        return ((filename.equals(other.filename)) &&
                (maxLength == other.maxLength) &&
                (preferMainLines == other.preferMainLines) &&
                (tournamentMode == other.tournamentMode) &&
                (random == other.random));
    }
    @Override
    public int hashCode() 
    {
        return 0;
    }
    
    public String filename = "";
    public int maxLength = 1000000;
    public boolean preferMainLines = false;
    public boolean tournamentMode = false;
    public double random = 0; // Scale probabilities according to p^(exp(-random))
}
