package ccc.chess.book;

import java.util.List;

import ccc.chess.book.C4aBook.BookEntry;

public class NullBook implements IOpeningBook 
{

    @Override
    public boolean enabled() {return false;}

    @Override
    public List<BookEntry> getBookEntries(Position pos) {return null;}

    @Override
    public void setOptions(BookOptions options) { }
}
