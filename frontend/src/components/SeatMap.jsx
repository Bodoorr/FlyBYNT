import { useMemo } from 'react';
import { BUSINESS_ROWS } from '../config.js';
import { initials } from '../format.js';

function parse(seatNumber) {
  const match = /^(\d+)([A-Z]+)$/i.exec(seatNumber ?? '');
  return match ? { row: Number(match[1]), letter: match[2].toUpperCase() } : null;
}

/**
 * Cabin grid built from seat numbers ("12C"). Seats assigned to a traveller show their initials.
 * assigned: Map seatId -> passenger
 */
export default function SeatMap({ seats, assigned, onSelect }) {
  const { rows, letters, aisleAfter } = useMemo(() => {
    const byRow = new Map();
    const allLetters = new Set();
    for (const seat of seats) {
      const pos = parse(seat.seatNumber);
      if (!pos) continue;
      allLetters.add(pos.letter);
      if (!byRow.has(pos.row)) byRow.set(pos.row, new Map());
      byRow.get(pos.row).set(pos.letter, seat);
    }
    const sortedLetters = [...allLetters].sort();
    return {
      rows: [...byRow.entries()].sort((a, b) => a[0] - b[0]),
      letters: sortedLetters,
      aisleAfter: Math.ceil(sortedLetters.length / 2) - 1,
    };
  }, [seats]);

  const columns = letters.flatMap((letter, i) => (i === aisleAfter ? [letter, 'aisle'] : [letter]));
  const gridTemplate = columns.map((c) => (c === 'aisle' ? 'var(--aisle)' : 'var(--seat)')).join(' ');

  return (
    <div className="seatmap-wrap">
      <div className="seatmap-legend" aria-hidden="true">
        <span><i className="legend legend-free" /> Available</span>
        <span><i className="legend legend-selected" /> Your pick</span>
        <span><i className="legend legend-taken" /> Taken</span>
      </div>

      <div className="cabin">
        <div className="cabin-nose" aria-hidden="true" />
        <div className="seat-grid" style={{ gridTemplateColumns: `var(--rowlabel) ${gridTemplate}` }}>
          <span />
          {columns.map((c, i) => (
            <span key={`h${i}`} className="seat-col-label">
              {c === 'aisle' ? '' : c}
            </span>
          ))}

          {rows.map(([row, rowSeats]) => (
            <Row
              key={row}
              row={row}
              rowSeats={rowSeats}
              columns={columns}
              assigned={assigned}
              onSelect={onSelect}
            />
          ))}
        </div>
      </div>
    </div>
  );
}

function Row({ row, rowSeats, columns, assigned, onSelect }) {
  const business = row <= BUSINESS_ROWS;
  return (
    <>
      {(row === 1 || row === BUSINESS_ROWS + 1) && (
        <div className="cabin-section" style={{ gridColumn: '1 / -1' }}>
          {business ? 'Business' : 'Economy'}
        </div>
      )}
      <span className="seat-row-label">{row}</span>
      {columns.map((c, i) => {
        if (c === 'aisle') return <span key={`a${i}`} />;
        const seat = rowSeats.get(c);
        if (!seat) return <span key={c} />;
        const owner = assigned.get(seat.seatId);
        const taken = seat.seatStatus !== 'AVAILABLE';
        const state = owner ? 'selected' : taken ? 'taken' : 'free';
        return (
          <button
            key={c}
            type="button"
            className={`seat seat-${state} ${business ? 'seat-business' : ''}`}
            disabled={taken && !owner}
            onClick={() => onSelect(seat)}
            aria-pressed={Boolean(owner)}
            aria-label={`Seat ${seat.seatNumber}${business ? ', business' : ''}, ${
              owner ? `selected for ${owner.firstName}` : taken ? 'taken' : 'available'
            }`}
            title={`${seat.seatNumber}${owner ? ` · ${owner.firstName}` : ''}`}
          >
            {owner ? initials(owner) : ''}
          </button>
        );
      })}
    </>
  );
}
