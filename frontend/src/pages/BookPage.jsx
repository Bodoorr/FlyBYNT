import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { api, newIdempotencyKey } from '../api.js';
import { AIRCRAFT_ID, DEFAULT_SEAT_SECTIONS } from '../config.js';
import FlightRoute from '../components/FlightRoute.jsx';
import Icon from '../components/Icon.jsx';
import PassengerForm from '../components/PassengerForm.jsx';
import SeatMap from '../components/SeatMap.jsx';
import { Alert, EmptyState, PageLoader, Spinner, StatusBadge } from '../components/ui.jsx';
import { formatDateLong, formatPrice, fullName, initials, isBookable } from '../format.js';

const STEPS = [
  { id: 'travellers', label: 'Travellers' },
  { id: 'seats', label: 'Seats' },
  { id: 'review', label: 'Review' },
];
const MAX_TRAVELLERS = 9;

export default function BookPage() {
  const { flightId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();

  const [flight, setFlight] = useState(location.state?.flight?.id === flightId ? location.state.flight : null);
  const [flightError, setFlightError] = useState('');
  const [passengers, setPassengers] = useState(null);
  const [passengersError, setPassengersError] = useState('');
  const [adding, setAdding] = useState(false);

  const [step, setStep] = useState('travellers');
  const [selectedIds, setSelectedIds] = useState([]);
  const [seatMap, setSeatMap] = useState({ status: 'idle' });
  const [assignments, setAssignments] = useState(new Map()); // passengerId -> seat
  const [activeId, setActiveId] = useState(null);

  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');
  const [results, setResults] = useState(null);
  // kept only while an outcome is unknown (network drop), so a retry can't book twice
  const pendingKeys = useRef(new Map());

  useEffect(() => {
    if (flight) return;
    api.getFlight(flightId).then(setFlight, (err) => setFlightError(err.message));
  }, [flightId, flight]);

  function loadPassengers() {
    setPassengersError('');
    api.myPassengers().then(setPassengers, (err) => setPassengersError(err.message));
  }
  useEffect(loadPassengers, []);

  const byId = useMemo(() => new Map((passengers ?? []).map((p) => [p.id, p])), [passengers]);
  const selected = selectedIds.map((id) => byId.get(id)).filter(Boolean);
  const seatOwners = useMemo(() => {
    const owners = new Map();
    for (const [pid, seat] of assignments) owners.set(seat.seatId, byId.get(pid));
    return owners;
  }, [assignments, byId]);

  async function loadSeats() {
    setSeatMap({ status: 'loading' });
    try {
      setSeatMap({ status: 'ready', seats: await api.getSeatMap(flight.id, AIRCRAFT_ID) });
    } catch (err) {
      setSeatMap(err.status === 404 ? { status: 'missing' } : { status: 'error', error: err.message });
    }
  }

  async function setUpSeatMap() {
    setSeatMap({ status: 'loading' });
    try {
      await api.generateSeats(AIRCRAFT_ID, DEFAULT_SEAT_SECTIONS);
    } catch (err) {
      // 409: another user created it first, which is fine
      if (err.status !== 409) {
        setSeatMap({ status: 'error', error: err.message });
        return;
      }
    }
    loadSeats();
  }

  function toggleTraveller(id) {
    setSelectedIds((ids) => {
      if (ids.includes(id)) return ids.filter((x) => x !== id);
      return ids.length >= MAX_TRAVELLERS ? ids : [...ids, id];
    });
  }

  function goTo(next) {
    setSubmitError('');
    if (next === 'seats') {
      // forget seats of travellers who were unselected
      setAssignments((a) => new Map([...a].filter(([pid]) => selectedIds.includes(pid))));
      setActiveId((current) =>
        selectedIds.includes(current) ? current : selectedIds.find((id) => !assignments.has(id)) ?? selectedIds[0],
      );
      if (seatMap.status === 'idle' || seatMap.status === 'error') loadSeats();
    }
    setStep(next);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function selectSeat(seat) {
    const owner = seatOwners.get(seat.seatId);
    if (owner) {
      // tapping your own seat frees it and hands focus back to that traveller
      setAssignments((a) => {
        const next = new Map(a);
        next.delete(owner.id);
        return next;
      });
      setActiveId(owner.id);
      return;
    }
    if (!activeId) return;
    const next = new Map(assignments);
    next.set(activeId, seat);
    setAssignments(next);
    setActiveId(selectedIds.find((id) => !next.has(id)) ?? activeId);
  }

  function keyFor(passengerId, seatId) {
    const k = `${passengerId}:${seatId}`;
    if (!pendingKeys.current.has(k)) pendingKeys.current.set(k, newIdempotencyKey());
    return pendingKeys.current.get(k);
  }

  async function confirm() {
    setSubmitting(true);
    setSubmitError('');
    const items = selected.map((p) => ({ passenger: p, seat: assignments.get(p.id) }));
    const requests = items.map(({ passenger, seat }) => ({
      flightId: flight.id,
      passengerId: passenger.id,
      seatId: seat.seatId,
      idempotencyKey: keyFor(passenger.id, seat.seatId),
    }));
    try {
      let outcome;
      if (requests.length === 1) {
        const booking = await api.createBooking(requests[0]);
        outcome = [{ ...items[0], booking }];
      } else {
        const res = await api.createFamilyBooking(requests);
        outcome = items.map((item) => {
          const r = res.find((x) => x.passengerId === item.passenger.id) ?? {};
          return { ...item, booking: r.booking, error: r.error };
        });
      }
      pendingKeys.current.clear();
      // a replayed request can hand back a booking that already failed
      setResults(
        outcome.map((o) =>
          o.booking?.status === 'FAILED' ? { ...o, booking: null, error: 'This booking could not be completed.' } : o,
        ),
      );
      setStep('done');
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch (err) {
      if (err.status !== 0) pendingKeys.current.clear();
      setSubmitError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  function rebookFailed() {
    const failedIds = results.filter((r) => !r.booking).map((r) => r.passenger.id);
    setSelectedIds(failedIds);
    setAssignments(new Map());
    setActiveId(failedIds[0]);
    setResults(null);
    setStep('seats');
    loadSeats();
  }

  if (flightError) {
    return (
      <div className="container section">
        <EmptyState
          icon="plane"
          title="We couldn't load this flight"
          action={<Link to="/" className="btn btn-primary">Back to search</Link>}
        >
          {flightError}
        </EmptyState>
      </div>
    );
  }
  if (!flight) return <PageLoader label="Loading flight" />;

  if (step === 'done') {
    return <Results flight={flight} results={results} onRebook={rebookFailed} />;
  }

  if (!isBookable(flight)) {
    return (
      <div className="container section">
        <EmptyState
          icon="clock"
          title="This flight can't be booked anymore"
          action={<Link to="/" className="btn btn-primary">Find another flight</Link>}
        >
          It has already departed or isn't on the schedule.
        </EmptyState>
      </div>
    );
  }

  const allSeated = selected.length > 0 && selected.every((p) => assignments.has(p.id));
  const total = flight.price * selected.length;
  const stepIndex = STEPS.findIndex((s) => s.id === step);
  const action = {
    travellers: { label: 'Choose seats', disabled: selected.length === 0, run: () => goTo('seats') },
    seats: { label: 'Review booking', disabled: !allSeated, run: () => goTo('review') },
    review: { label: 'Confirm & book', disabled: submitting || !allSeated, run: confirm },
  }[step];

  return (
    <div className="container section book">
      <button type="button" className="back-link" onClick={() => (stepIndex > 0 ? goTo(STEPS[stepIndex - 1].id) : navigate(-1))}>
        <Icon name="arrowLeft" size={16} /> {stepIndex > 0 ? STEPS[stepIndex - 1].label : 'Back to flights'}
      </button>

      <ol className="stepper">
        {STEPS.map((s, i) => (
          <li key={s.id} className={i < stepIndex ? 'done' : i === stepIndex ? 'current' : ''}>
            <button type="button" disabled={i >= stepIndex} onClick={() => goTo(s.id)}>
              <span className="stepper-dot">{i < stepIndex ? <Icon name="check" size={14} /> : i + 1}</span>
              {s.label}
            </button>
          </li>
        ))}
      </ol>

      <div className="book-layout">
        <div className="book-main">
          {step === 'travellers' && (
            <section className="card pad">
              <div className="card-head">
                <div>
                  <h2>Who's flying?</h2>
                  <p className="muted">Pick everyone travelling together. Each traveller gets their own seat and booking.</p>
                </div>
                {!adding && passengers?.length > 0 && (
                  <button type="button" className="btn btn-ghost btn-sm" onClick={() => setAdding(true)}>
                    <Icon name="plus" size={16} /> Add
                  </button>
                )}
              </div>

              {passengersError && <Alert onRetry={loadPassengers}>{passengersError}</Alert>}
              {!passengers && !passengersError && <PageLoader label="Loading passengers" />}

              {passengers && (passengers.length === 0 || adding) && (
                <div className="inline-form">
                  <h3>{passengers.length === 0 ? 'Add your first traveller' : 'New traveller'}</h3>
                  <PassengerForm
                    onSaved={(p) => {
                      setPassengers((list) => [...list, p]);
                      setSelectedIds((ids) => (ids.length < MAX_TRAVELLERS ? [...ids, p.id] : ids));
                      setAdding(false);
                    }}
                    onCancel={passengers.length > 0 ? () => setAdding(false) : undefined}
                  />
                </div>
              )}

              {passengers?.length > 0 && (
                <ul className="traveller-list">
                  {passengers.map((p) => {
                    const checked = selectedIds.includes(p.id);
                    return (
                      <li key={p.id}>
                        <label className={`traveller ${checked ? 'traveller-on' : ''}`}>
                          <input type="checkbox" checked={checked} onChange={() => toggleTraveller(p.id)} />
                          <span className="avatar">{initials(p)}</span>
                          <span className="traveller-text">
                            <strong>{fullName(p)}</strong>
                            <span className="muted small">
                              {p.nationality} · born {formatDateLong(p.dateOfBirth)}
                            </span>
                          </span>
                          <span className="check-box" aria-hidden="true">
                            <Icon name="check" size={14} />
                          </span>
                        </label>
                      </li>
                    );
                  })}
                </ul>
              )}
              {selectedIds.length >= MAX_TRAVELLERS && (
                <p className="muted small">Up to {MAX_TRAVELLERS} travellers per booking.</p>
              )}
            </section>
          )}

          {step === 'seats' && (
            <section className="card pad">
              <div className="card-head">
                <div>
                  <h2>Choose seats</h2>
                  <p className="muted">Tap a traveller, then a seat. Tap a chosen seat again to free it.</p>
                </div>
              </div>

              <div className="seat-assign" role="radiogroup" aria-label="Traveller choosing a seat">
                {selected.map((p) => {
                  const seat = assignments.get(p.id);
                  return (
                    <button
                      key={p.id}
                      type="button"
                      role="radio"
                      aria-checked={activeId === p.id}
                      className={`assign-chip ${activeId === p.id ? 'assign-chip-active' : ''}`}
                      onClick={() => setActiveId(p.id)}
                    >
                      <span className="avatar avatar-sm">{initials(p)}</span>
                      <span>{p.firstName}</span>
                      <span className={`assign-seat ${seat ? '' : 'muted'}`}>{seat ? seat.seatNumber : '—'}</span>
                    </button>
                  );
                })}
              </div>

              {seatMap.status === 'loading' && <PageLoader label="Loading seat map" />}
              {seatMap.status === 'error' && <Alert onRetry={loadSeats}>{seatMap.error}</Alert>}
              {seatMap.status === 'missing' && (
                <EmptyState
                  icon="seat"
                  title="No seat map yet"
                  action={
                    <button type="button" className="btn btn-primary" onClick={setUpSeatMap}>
                      Set up seat map
                    </button>
                  }
                >
                  The aircraft for this flight has no seats configured. Create the standard A320 layout to continue.
                </EmptyState>
              )}
              {seatMap.status === 'ready' && (
                <SeatMap seats={seatMap.seats} assigned={seatOwners} onSelect={selectSeat} />
              )}
            </section>
          )}

          {step === 'review' && (
            <section className="card pad">
              <h2>Review and confirm</h2>
              <p className="muted">Seats are held and confirmed the moment you book.</p>
              <div className="review-flight">
                <div className="review-flight-head">
                  <strong>{flight.flightNumber}</strong>
                  <span className="muted">{formatDateLong(flight.departureTime)}</span>
                </div>
                <FlightRoute flight={flight} />
              </div>
              <ul className="review-list">
                {selected.map((p) => (
                  <li key={p.id}>
                    <span className="avatar avatar-sm">{initials(p)}</span>
                    <span className="review-name">{fullName(p)}</span>
                    <span className="seat-tag">
                      <Icon name="seat" size={14} /> {assignments.get(p.id)?.seatNumber}
                    </span>
                    <span className="review-price">{formatPrice(flight.price)}</span>
                  </li>
                ))}
              </ul>
              <div className="review-total">
                <span>Total</span>
                <strong>{formatPrice(total)}</strong>
              </div>
              {submitError && (
                <Alert>
                  {submitError}
                  <div className="alert-actions">
                    <button type="button" className="btn btn-ghost btn-sm" onClick={() => { loadSeats(); goTo('seats'); }}>
                      Change seats
                    </button>
                  </div>
                </Alert>
              )}
            </section>
          )}
        </div>

        <aside className="summary">
          <div className="summary-details card pad">
            <div className="summary-flight">
              <span className="airline-badge">{flight.airline?.iataCode ?? '—'}</span>
              <div>
                <strong>{flight.flightNumber}</strong>
                <span className="muted small">{formatDateLong(flight.departureTime)}</span>
              </div>
            </div>
            <FlightRoute flight={flight} compact />
            <dl className="summary-lines">
              <div>
                <dt>Fare per traveller</dt>
                <dd>{formatPrice(flight.price)}</dd>
              </div>
              <div>
                <dt>Travellers</dt>
                <dd>{selected.length}</dd>
              </div>
            </dl>
          </div>
          <div className="summary-bar card">
            <div className="summary-total">
              <span className="muted small">Total</span>
              <strong>{formatPrice(total)}</strong>
            </div>
            <button type="button" className="btn btn-primary btn-lg" disabled={action.disabled} onClick={action.run}>
              {submitting ? <Spinner label="Booking" /> : action.label}
            </button>
          </div>
        </aside>
      </div>
    </div>
  );
}

function Results({ flight, results, onRebook }) {
  const booked = results.filter((r) => r.booking);
  const failed = results.filter((r) => !r.booking);
  const pending = booked.some((r) => r.booking.status === 'PENDING');
  const allGood = failed.length === 0;

  return (
    <div className="container section narrow">
      <div className={`result-hero ${allGood ? 'ok' : booked.length ? 'partial' : 'bad'}`}>
        <span className="result-icon">
          <Icon name={allGood ? 'check' : 'alert'} size={30} />
        </span>
        <h1>{allGood ? (pending ? 'Almost there' : "You're booked!") : booked.length ? 'Partly booked' : 'Booking failed'}</h1>
        <p>
          {flight.flightNumber} · {flight.departure?.iataCode} → {flight.arrival?.iataCode} ·{' '}
          {formatDateLong(flight.departureTime)}
        </p>
        {pending && <p className="small">Some seats are still being confirmed. Check My trips in a minute.</p>}
      </div>

      <ul className="card result-list">
        {results.map((r) => (
          <li key={r.passenger.id}>
            <span className="avatar">{initials(r.passenger)}</span>
            <div className="result-text">
              <strong>{fullName(r.passenger)}</strong>
              {r.booking ? (
                <span className="muted small">
                  Seat {r.seat.seatNumber} · Ref <span className="mono">{r.booking.bookingReference}</span>
                </span>
              ) : (
                <span className="text-danger small">{r.error}</span>
              )}
            </div>
            {r.booking ? <StatusBadge status={r.booking.status} /> : <StatusBadge status="FAILED" />}
          </li>
        ))}
      </ul>

      <div className="result-actions">
        {failed.length > 0 && (
          <button type="button" className="btn btn-primary" onClick={onRebook}>
            Pick new seats for {failed.length === 1 ? failed[0].passenger.firstName : `${failed.length} travellers`}
          </button>
        )}
        <Link to="/bookings" className={`btn ${failed.length ? 'btn-ghost' : 'btn-primary'}`}>
          View my trips
        </Link>
        <Link to="/" className="btn btn-ghost">
          Search flights
        </Link>
      </div>
    </div>
  );
}
