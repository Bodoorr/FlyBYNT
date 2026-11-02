import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import FlightRoute from '../components/FlightRoute.jsx';
import Icon from '../components/Icon.jsx';
import { Alert, ConfirmDialog, EmptyState, PageLoader, StatusBadge } from '../components/ui.jsx';
import { formatDateLong, formatPrice, fullName } from '../format.js';

const FILTERS = [
  { id: 'upcoming', label: 'Upcoming' },
  { id: 'all', label: 'All' },
  { id: 'cancelled', label: 'Cancelled' },
];

export default function BookingsPage() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState('upcoming');
  const [toCancel, setToCancel] = useState(null);
  const [cancelling, setCancelling] = useState(false);
  const [notice, setNotice] = useState(null);

  // bookings only hold ids, so flights, seats and passengers are looked up alongside
  const load = useCallback(async () => {
    setError('');
    try {
      const [bookings, passengers] = await Promise.all([api.myBookings(), api.myPassengers().catch(() => [])]);
      const flightIds = [...new Set(bookings.map((b) => b.flightId))];
      const flights = new Map(
        await Promise.all(flightIds.map((id) => api.getFlight(id).then((f) => [id, f], () => [id, null]))),
      );
      const seats = new Map(
        await Promise.all(
          bookings.map((b) =>
            api.getSeat(b.flightId, b.seatId).then((s) => [b.id, s.seatNumber], () => [b.id, null]),
          ),
        ),
      );
      setData({ bookings, flights, seats, passengers: new Map(passengers.map((p) => [p.id, p])) });
    } catch (err) {
      setError(err.message);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const shown = useMemo(() => {
    if (!data) return [];
    const now = new Date();
    return data.bookings.filter((b) => {
      if (filter === 'cancelled') return b.status === 'CANCELLED';
      if (filter === 'upcoming') {
        const dep = data.flights.get(b.flightId)?.departureTime;
        return (b.status === 'CONFIRMED' || b.status === 'PENDING') && (!dep || new Date(dep) > now);
      }
      return true;
    });
  }, [data, filter]);

  async function cancel() {
    setCancelling(true);
    try {
      const updated = await api.cancelBooking(toCancel.id);
      setData((d) => ({ ...d, bookings: d.bookings.map((b) => (b.id === updated.id ? updated : b)) }));
      setNotice({ kind: 'success', text: `Booking ${updated.bookingReference} was cancelled and the seat released.` });
    } catch (err) {
      setNotice({ kind: 'error', text: err.message });
    } finally {
      setCancelling(false);
      setToCancel(null);
    }
  }

  return (
    <div className="container section">
      <div className="page-head">
        <div>
          <h1>My trips</h1>
          <p className="muted">Every booking you've made, one card per traveller.</p>
        </div>
        <button type="button" className="btn btn-ghost btn-sm" onClick={load}>
          <Icon name="refresh" size={16} /> Refresh
        </button>
      </div>

      <div className="tabs" role="tablist">
        {FILTERS.map((f) => (
          <button
            key={f.id}
            type="button"
            role="tab"
            aria-selected={filter === f.id}
            className={`tab ${filter === f.id ? 'tab-active' : ''}`}
            onClick={() => setFilter(f.id)}
          >
            {f.label}
          </button>
        ))}
      </div>

      {notice && <Alert kind={notice.kind}>{notice.text}</Alert>}
      {error && <Alert onRetry={load}>{error}</Alert>}
      {!data && !error && <PageLoader label="Loading your trips" />}

      {data && shown.length === 0 && (
        <EmptyState
          icon="ticket"
          title={filter === 'upcoming' ? 'No upcoming trips' : 'Nothing here yet'}
          action={<Link to="/" className="btn btn-primary">Find a flight</Link>}
        >
          {filter === 'upcoming' ? 'Booked flights will show up here.' : 'Bookings matching this filter will show up here.'}
        </EmptyState>
      )}

      {data && shown.length > 0 && (
        <ul className="booking-list">
          {shown.map((b) => {
            const flight = data.flights.get(b.flightId);
            return (
              <li key={b.id} className={`card booking-card status-${b.status.toLowerCase()}`}>
                <div className="booking-head">
                  <div>
                    <span className="muted small">Booking reference</span>
                    <strong className="mono booking-ref">{b.bookingReference}</strong>
                  </div>
                  <StatusBadge status={b.status} />
                </div>

                {flight ? (
                  <>
                    <div className="booking-flight">
                      <span className="airline-badge">{flight.airline?.iataCode ?? '—'}</span>
                      <strong>{flight.flightNumber}</strong>
                      <span className="muted">· {formatDateLong(flight.departureTime)}</span>
                    </div>
                    <FlightRoute flight={flight} compact />
                  </>
                ) : (
                  <p className="muted">Flight details unavailable</p>
                )}

                <dl className="booking-facts">
                  <div>
                    <dt>Traveller</dt>
                    <dd>{fullName(data.passengers.get(b.passengerId))}</dd>
                  </div>
                  <div>
                    <dt>Seat</dt>
                    <dd>{data.seats.get(b.id) ?? '—'}</dd>
                  </div>
                  <div>
                    <dt>Paid</dt>
                    <dd>{formatPrice(b.totalPrice)}</dd>
                  </div>
                </dl>

                {b.status === 'PENDING' && (
                  <p className="muted small booking-note">
                    <Icon name="clock" size={14} /> We're confirming this seat. It updates within a few minutes.
                  </p>
                )}
                {b.status === 'CONFIRMED' && (
                  <div className="booking-actions">
                    <button type="button" className="btn btn-danger-ghost btn-sm" onClick={() => setToCancel(b)}>
                      Cancel booking
                    </button>
                  </div>
                )}
              </li>
            );
          })}
        </ul>
      )}

      {toCancel && (
        <ConfirmDialog
          title="Cancel this booking?"
          confirmLabel="Yes, cancel"
          danger
          busy={cancelling}
          onConfirm={cancel}
          onCancel={() => setToCancel(null)}
        >
          Booking <strong className="mono">{toCancel.bookingReference}</strong> for{' '}
          {fullName(data.passengers.get(toCancel.passengerId))} will be cancelled and seat{' '}
          {data.seats.get(toCancel.id) ?? ''} released. This can't be undone.
        </ConfirmDialog>
      )}
    </div>
  );
}
