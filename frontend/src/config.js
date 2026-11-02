// Seat-service keeps seat layouts per aircraft, but AirLabs flights don't carry our aircraft ids yet,
// so every flight uses this layout for now.
export const AIRCRAFT_ID = 'A320-1';

// Layout created by "Set up seat map" when the aircraft has no seats yet
export const DEFAULT_SEAT_SECTIONS = [
  { seatClass: 'BUSINESS', fromRow: 1, toRow: 3, letters: 'ACDF', windowLetters: 'AF', aisleLetters: 'CD' },
  { seatClass: 'ECONOMY', fromRow: 4, toRow: 30, letters: 'ABCDEF', windowLetters: 'AF', aisleLetters: 'CD' },
];

export const BUSINESS_ROWS = 3;

export const CURRENCY = 'BHD';

export const POPULAR_AIRPORTS = ['BAH', 'DXB', 'DOH', 'RUH', 'KWI', 'MCT'];
