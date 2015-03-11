CREATE INDEX idx_white ON pgn(White, Date, Event, Site, _id);
CREATE INDEX idx_black ON pgn(Black, Date, Event, Site, _id);
CREATE INDEX idx_event ON pgn(Event, Site, _id);
CREATE INDEX idx_eco ON pgn(ECO, Opening, _id);
