import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { POPULAR_AIRPORTS } from '../config.js';
import Icon from '../components/Icon.jsx';
import FlightRoute from '../components/FlightRoute.jsx';
import { Alert, EmptyState, PageLoader, Spinner, StatusBadge } from '../components/ui.jsx';
import { flightStatusLabel, formatPrice, isBookable } from '../format.js';

const CACHE_KEY = 'flybynt.lastSearch';

// Each search spends one AirLabs request, so the last result survives navigation
function readCache() {
  try {
    return JSON.parse(sessionStorage.getItem(CACHE_KEY)) || null;
  } catch {
    return null;
  }
}

function writeCache(value) {
  try {
    sessionStorage.setItem(CACHE_KEY, JSON.stringify(value));
  } catch {
    // not critical
  }
}

export default function SearchPage() {
  const navigate = useNavigate();
  const [result, setResult] = useState(readCache);
  const [airport, setAirport] = useState(() => result?.airport ?? 'BAH');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [onlyBookable, setOnlyBookable] = useState(true);

  async function search(code = airport) {
    const iata = code.trim().toUpperCase();
    if (!/^[A-Z]{3}$/.test(iata)) {
      setError('Enter a 3-letter airport code, like BAH.');
      return;
    }
    setAirport(iata);
    setError('');
    setLoading(true);
    try {
      const flights = await api.searchFlights(iata);
      // the same flight can appear twice in a schedule; keep one card per id
      const unique = [...new Map(flights.map((f) => [f.id, f])).values()].sort(
        (a, b) => new Date(a.departureTime ?? 0) - new Date(b.departureTime ?? 0),
      );
      const next = { airport: iata, flights: unique, at: Date.now() };
      setResult(next);
      writeCache(next);
    } catch (err) {
      setError(err.status === 502 ? 'The flight data provider did not answer. Try again shortly.' : err.message);
    } finally {
      setLoading(false);
    }
  }

  const shown = useMemo(() => {
    const flights = result?.flights ?? [];
    return onlyBookable ? flights.filter(isBookable) : flights;
  }, [result, onlyBookable]);

  return (
    <>
      <section className="hero">
        <div className="container">
          <h1>Where are you flying from?</h1>
          <p>Live departures with real-time seat availability.</p>
          <form
            className="search-box"
            onSubmit={(e) => {
              e.preventDefault();
              search();
            }}
          >
            <label className="search-field">
              <Icon name="plane" size={20} />
              <span className="visually-hidden">Departure airport code</span>
              <input
                value={airport}
                onChange={(e) => setAirport(e.target.value.toUpperCase())}
                maxLength={3}
                placeholder="BAH"
                autoCapitalize="characters"
                spellCheck="false"
              />
              <span className="search-hint">Departure airport</span>
            </label>
            <button type="submit" className="btn btn-accent btn-lg" disabled={loading}>
              {loading ? <Spinner label="Searching" /> : <Icon name="search" size={18} />}
              <span>Search</span>
            </button>
          </form>
          <div className="chips" aria-label="Popular airports">
            {POPULAR_AIRPORTS.map((code) => (
              <button
                key={code}
                type="button"
                className={`chip ${result?.airport === code ? 'chip-active' : ''}`}
                onClick={() => search(code)}
                disabled={loading}
              >
                {code}
              </button>
            ))}
          </div>
        </div>
      </section>

      <div className="container section">
        {error && <Alert>{error}</Alert>}

        {loading && !result && <PageLoader label="Finding flights" />}

        {result && (
          <>
            <div className="results-head">
              <div>
                <h2>Departures from {result.airport}</h2>
                <p className="muted">
                  {shown.length} {onlyBookable ? 'bookable ' : ''}flight{shown.length === 1 ? '' : 's'}
                  {result.at && ` · updated ${new Date(result.at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`}
                </p>
              </div>
              <label className="switch">
                <input type="checkbox" checked={onlyBookable} onChange={(e) => setOnlyBookable(e.target.checked)} />
                <span className="switch-track" aria-hidden="true" />
                Only bookable
              </label>
            </div>

            {shown.length === 0 ? (
              <EmptyState icon="plane" title="No flights to show">
                {onlyBookable
                  ? 'Nothing bookable from this airport right now. Try another airport or show all flights.'
                  : 'The schedule came back empty for this airport.'}
              </EmptyState>
            ) : (
              <ul className={`flight-list ${loading ? 'is-loading' : ''}`}>
                {shown.map((flight) => (
                  <FlightCard
                    key={flight.id}
                    flight={flight}
                    onSelect={() => navigate(`/book/${flight.id}`, { state: { flight } })}
                  />
                ))}
              </ul>
            )}
          </>
        )}

        {!result && !loading && !error && (
          <EmptyState icon="search" title="Search a departure airport">
            Pick one of the popular airports above or type any 3-letter IATA code.
          </EmptyState>
        )}
      </div>
    </>
  );
}

function FlightCard({ flight, onSelect }) {
  const bookable = isBookable(flight);
  return (
    <li className={`card flight-card ${bookable ? '' : 'flight-card-off'}`}>
      <div className="flight-airline">
        <span className="airline-badge">{flight.airline?.iataCode ?? '—'}</span>
        <div>
          <strong>{flight.flightNumber}</strong>
          <span className="muted small">{flight.aircraft?.icaoCode ?? 'Aircraft TBA'}</span>
        </div>
      </div>
      <FlightRoute flight={flight} />
      <div className="flight-buy">
        {bookable ? <StatusBadge status="Scheduled" /> : <span className="badge badge-neutral">{flightStatusLabel(flight)}</span>}
        <span className="price">{formatPrice(flight.price)}</span>
        <button type="button" className="btn btn-primary" onClick={onSelect} disabled={!bookable}>
          Select
          <Icon name="arrowRight" size={16} />
        </button>
      </div>
    </li>
  );
}
