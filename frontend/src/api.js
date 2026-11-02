// One client for all five services. Requests go to /api/<service>/..., which nginx (Docker)
// or the Vite dev server forwards to the right service.

const TOKEN_KEY = 'flybynt.token';

export function readToken() {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function storeToken(token) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch {
    // storage blocked (private mode): the session just won't survive a reload
  }
}

/** Claims of a JWT without verifying it; the services verify. */
export function decodeToken(token) {
  try {
    const part = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = part.padEnd(part.length + ((4 - (part.length % 4)) % 4), '=');
    const bytes = Uint8Array.from(atob(padded), (c) => c.charCodeAt(0));
    return JSON.parse(new TextDecoder().decode(bytes));
  } catch {
    return null;
  }
}

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

let onUnauthorized = () => {};
export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

const FALLBACK_MESSAGES = {
  400: 'Some details are invalid.',
  401: 'Your session has expired. Please sign in again.',
  403: "You don't have access to that.",
  404: 'Not found.',
  409: 'That conflicts with something that already exists.',
  503: 'A service is temporarily unavailable. Please try again.',
};

// Services answer errors differently: {error}, {message}, or Spring's default {error: "Conflict", ...}
function messageFrom(status, data) {
  if (data && typeof data === 'object') {
    if (data.message && typeof data.message === 'string') return data.message;
    if (data.error && typeof data.error === 'string' && data.error.includes(' ')) return data.error;
  }
  return FALLBACK_MESSAGES[status] || 'Something went wrong. Please try again.';
}

async function request(service, path, { method = 'GET', body, auth = true } = {}) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const token = readToken();
  if (auth && token) headers.Authorization = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(`/api/${service}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, "Can't reach the server. Check that the services are running.");
  }

  const text = await res.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!res.ok) {
    if (res.status === 401 && auth) onUnauthorized();
    throw new ApiError(res.status, messageFrom(res.status, data));
  }
  return data;
}

const enc = encodeURIComponent;

export const api = {
  login: (email, password) =>
    request('auth', '/auth/users/login', { method: 'POST', body: { email, password }, auth: false }),
  register: (user) => request('auth', '/auth/users/register', { method: 'POST', body: user, auth: false }),

  searchFlights: (airport) => request('flight', `/flights?departure=${enc(airport)}`),
  getFlight: (id) => request('flight', `/flights/${enc(id)}`),

  getSeatMap: (flightId, aircraftId) =>
    request('seat', `/flights/${enc(flightId)}/seats?aircraftId=${enc(aircraftId)}`),
  getSeat: (flightId, seatId) => request('seat', `/flights/${enc(flightId)}/seats/${enc(seatId)}`),
  generateSeats: (aircraftId, sections) =>
    request('seat', `/aircraft/${enc(aircraftId)}/seats/generate`, { method: 'POST', body: { sections } }),

  myPassengers: () => request('passenger', '/passengers/me'),
  createPassenger: (passenger) => request('passenger', '/passengers', { method: 'POST', body: passenger }),
  deletePassenger: (id) => request('passenger', `/passengers/${enc(id)}`, { method: 'DELETE' }),

  myBookings: () => request('booking', '/bookings/me'),
  createBooking: (booking) => request('booking', '/bookings', { method: 'POST', body: booking }),
  createFamilyBooking: (bookings) => request('booking', '/bookings/family', { method: 'POST', body: { bookings } }),
  cancelBooking: (id) => request('booking', `/bookings/${enc(id)}/cancel`, { method: 'POST' }),
};

/** Unique key per booking attempt, so a retried request can't create a second booking. */
export function newIdempotencyKey() {
  if (globalThis.crypto?.randomUUID) return crypto.randomUUID();
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`;
}
