import { CURRENCY } from './config.js';

export const formatTime = (iso) =>
  iso ? new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '--:--';

export const formatDate = (iso) =>
  iso ? new Date(iso).toLocaleDateString([], { weekday: 'short', day: 'numeric', month: 'short' }) : '';

export const formatDateLong = (iso) =>
  iso ? new Date(iso).toLocaleDateString([], { day: 'numeric', month: 'long', year: 'numeric' }) : '';

export function formatDuration(from, to) {
  if (!from || !to) return '';
  const minutes = Math.round((new Date(to) - new Date(from)) / 60000);
  return `${Math.floor(minutes / 60)}h ${String(minutes % 60).padStart(2, '0')}m`;
}

export function formatPrice(amount) {
  if (amount == null) return '—';
  return `${CURRENCY} ${Number(amount).toFixed(2)}`;
}

/** Booking-service only accepts flights that are scheduled and haven't left yet. */
export function isBookable(flight) {
  return (
    flight?.status === 'scheduled' &&
    flight.departureTime &&
    flight.arrivalTime &&
    flight.price != null &&
    new Date(flight.departureTime) > new Date()
  );
}

export function flightStatusLabel(flight) {
  if (isBookable(flight)) return 'Scheduled';
  if (flight?.status === 'active' || flight?.status === 'en-route') return 'In the air';
  if (flight?.status === 'landed') return 'Landed';
  if (flight?.status === 'cancelled') return 'Cancelled';
  return 'Departed';
}

export const fullName = (p) => (p ? `${p.firstName} ${p.lastName}` : 'Unknown passenger');

export const initials = (p) => (p ? `${p.firstName?.[0] ?? ''}${p.lastName?.[0] ?? ''}`.toUpperCase() : '?');
