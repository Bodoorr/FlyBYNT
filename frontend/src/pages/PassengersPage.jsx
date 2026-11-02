import { useEffect, useState } from 'react';
import { api } from '../api.js';
import Icon from '../components/Icon.jsx';
import PassengerForm from '../components/PassengerForm.jsx';
import { Alert, ConfirmDialog, EmptyState, PageLoader } from '../components/ui.jsx';
import { formatDateLong, fullName, initials } from '../format.js';

export default function PassengersPage() {
  const [passengers, setPassengers] = useState(null);
  const [error, setError] = useState('');
  const [adding, setAdding] = useState(false);
  const [toDelete, setToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  function load() {
    setError('');
    api.myPassengers().then(setPassengers, (err) => setError(err.message));
  }
  useEffect(load, []);

  async function remove() {
    setDeleting(true);
    try {
      await api.deletePassenger(toDelete.id);
      setPassengers((list) => list.filter((p) => p.id !== toDelete.id));
    } catch (err) {
      setError(err.message);
    } finally {
      setDeleting(false);
      setToDelete(null);
    }
  }

  return (
    <div className="container section">
      <div className="page-head">
        <div>
          <h1>Passengers</h1>
          <p className="muted">Save the people you travel with to book them in seconds.</p>
        </div>
        {!adding && passengers?.length > 0 && (
          <button type="button" className="btn btn-primary" onClick={() => setAdding(true)}>
            <Icon name="plus" size={18} /> Add passenger
          </button>
        )}
      </div>

      {error && <Alert onRetry={load}>{error}</Alert>}
      {!passengers && !error && <PageLoader label="Loading passengers" />}

      {passengers && (adding || passengers.length === 0) && (
        <section className="card pad inline-form">
          <h2>{passengers.length === 0 ? 'Add your first passenger' : 'New passenger'}</h2>
          <PassengerForm
            onSaved={(p) => {
              setPassengers((list) => [...list, p]);
              setAdding(false);
            }}
            onCancel={passengers.length > 0 ? () => setAdding(false) : undefined}
          />
        </section>
      )}

      {passengers?.length > 0 && (
        <ul className="passenger-grid">
          {passengers.map((p) => (
            <li key={p.id} className="card passenger-card">
              <span className="avatar avatar-lg">{initials(p)}</span>
              <div className="passenger-text">
                <strong>{fullName(p)}</strong>
                <span className="muted small">{p.nationality}</span>
                <span className="muted small">Born {formatDateLong(p.dateOfBirth)}</span>
              </div>
              <button
                type="button"
                className="icon-btn"
                aria-label={`Remove ${fullName(p)}`}
                onClick={() => setToDelete(p)}
              >
                <Icon name="trash" size={18} />
              </button>
            </li>
          ))}
        </ul>
      )}

      {passengers?.length === 0 && !adding && (
        <EmptyState icon="users" title="No passengers yet">
          Add yourself and anyone you travel with.
        </EmptyState>
      )}

      {toDelete && (
        <ConfirmDialog
          title={`Remove ${fullName(toDelete)}?`}
          confirmLabel="Remove"
          danger
          busy={deleting}
          onConfirm={remove}
          onCancel={() => setToDelete(null)}
        >
          They won't appear when you book. Existing bookings for them stay as they are.
        </ConfirmDialog>
      )}
    </div>
  );
}
