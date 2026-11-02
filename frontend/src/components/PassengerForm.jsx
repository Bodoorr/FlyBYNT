import { useState } from 'react';
import { api } from '../api.js';
import { Alert, Spinner } from './ui.jsx';

const EMPTY = { firstName: '', lastName: '', passportNumber: '', dateOfBirth: '', nationality: '' };
const today = () => new Date().toISOString().slice(0, 10);

/** Creates a passenger via passenger-service and hands the saved passenger back. */
export default function PassengerForm({ onSaved, onCancel }) {
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const set = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  async function submit(e) {
    e.preventDefault();
    setError('');
    if (form.dateOfBirth >= today()) {
      setError('Date of birth must be in the past.');
      return;
    }
    setSaving(true);
    try {
      const saved = await api.createPassenger({
        ...form,
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        passportNumber: form.passportNumber.trim().toUpperCase(),
        nationality: form.nationality.trim(),
      });
      setForm(EMPTY);
      onSaved(saved);
    } catch (err) {
      setError(err.status === 500 ? 'Could not save. That passport number may already be registered.' : err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="form passenger-form" onSubmit={submit}>
      {error && <Alert>{error}</Alert>}
      <div className="form-row">
        <label className="field">
          <span>First name</span>
          <input value={form.firstName} onChange={set('firstName')} required autoComplete="given-name" />
        </label>
        <label className="field">
          <span>Last name</span>
          <input value={form.lastName} onChange={set('lastName')} required autoComplete="family-name" />
        </label>
      </div>
      <div className="form-row">
        <label className="field">
          <span>Passport number</span>
          <input value={form.passportNumber} onChange={set('passportNumber')} required autoCapitalize="characters" />
        </label>
        <label className="field">
          <span>Date of birth</span>
          <input type="date" value={form.dateOfBirth} onChange={set('dateOfBirth')} max={today()} required />
        </label>
      </div>
      <label className="field">
        <span>Nationality</span>
        <input value={form.nationality} onChange={set('nationality')} required placeholder="e.g. Bahraini" />
      </label>
      <div className="form-actions">
        {onCancel && (
          <button type="button" className="btn btn-ghost" onClick={onCancel} disabled={saving}>
            Cancel
          </button>
        )}
        <button type="submit" className="btn btn-primary" disabled={saving}>
          {saving ? <Spinner label="Saving" /> : 'Save passenger'}
        </button>
      </div>
    </form>
  );
}
