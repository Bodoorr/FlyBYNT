import Icon from './Icon.jsx';
import { formatDate, formatDuration, formatTime } from '../format.js';

/** BAH 14:25 ——✈—— DEL 20:45, the visual core of every flight and booking card. */
export default function FlightRoute({ flight, compact = false }) {
  const from = flight?.departure?.iataCode ?? '???';
  const to = flight?.arrival?.iataCode ?? '???';
  return (
    <div className={`route ${compact ? 'route-compact' : ''}`}>
      <div className="route-end">
        <span className="route-time">{formatTime(flight?.departureTime)}</span>
        <span className="route-code">{from}</span>
        {!compact && <span className="route-date">{formatDate(flight?.departureTime)}</span>}
      </div>
      <div className="route-line" aria-hidden="true">
        <span className="route-duration">{formatDuration(flight?.departureTime, flight?.arrivalTime)}</span>
        <span className="route-track">
          <Icon name="plane" size={16} className="route-plane" />
        </span>
        <span className="route-direct">Direct</span>
      </div>
      <div className="route-end route-end-right">
        <span className="route-time">{formatTime(flight?.arrivalTime)}</span>
        <span className="route-code">{to}</span>
        {!compact && <span className="route-date">{formatDate(flight?.arrivalTime)}</span>}
      </div>
      <span className="visually-hidden">
        From {from} at {formatTime(flight?.departureTime)} to {to} at {formatTime(flight?.arrivalTime)}
      </span>
    </div>
  );
}
