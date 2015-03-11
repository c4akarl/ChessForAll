
package ccc.chess.book;

import java.util.List;

import ccc.chess.book.C4aBook.BookEntry;

interface IOpeningBook {
    /** Return true if book is currently enabled. */
    boolean enabled();

    /** Set book options, including filename. */
    void setOptions(BookOptions options);

    /** Get all book entries for a position. */
    List<BookEntry> getBookEntries(Position pos);
}
