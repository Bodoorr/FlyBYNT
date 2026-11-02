import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import Icon from '../components/Icon.jsx';
import { Alert, Spinner } from '../components/ui.jsx';

export default function AuthPage() {
  const { token, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '', mobileNumber: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (token) return <Navigate to={location.state?.from || '/'} replace />;

  const set = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  function switchMode(next) {
    setMode(next);
    setError('');
  }

  async function submit(e) {
    e.preventDefault();
    setError('');
    if (mode === 'register' && form.password.length < 8) {
      setError('Use at least 8 characters for your password.');
      return;
    }
    setBusy(true);
    const email = form.email.trim().toLowerCase();
    try {
      if (mode === 'register') {
        await api.register({
          firstName: form.firstName.trim(),
          lastName: form.lastName.trim(),
          email,
          password: form.password,
          mobileNumber: form.mobileNumber.trim() || null,
        });
      }
      await login(email, form.password);
      navigate(location.state?.from || '/', { replace: true });
    } catch (err) {
      if (mode === 'login' && err.status === 401) setError('Wrong email or password.');
      else if (mode === 'register' && err.status === 409) setError('An account with this email already exists. Sign in instead.');
      else setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth">
      <section className="auth-hero" aria-hidden="true">
        <div className="auth-hero-inner">
          <span className="brand brand-light">
            <span className="brand-mark">
              <Icon name="plane" size={18} />
            </span>
            FlyBYNT
          </span>
          <h1>Your next trip, one seat away.</h1>
          <p>Live schedules, real-time seat maps and instant confirmation for you and your family.</p>
          <div className="auth-hero-plane">
            <Icon name="plane" size={120} />
          </div>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-card">
          <span className="brand auth-brand-mobile">
            <span className="brand-mark">
              <Icon name="plane" size={18} />
            </span>
            FlyBYNT
          </span>
          <h2>{mode === 'login' ? 'Welcome back' : 'Create your account'}</h2>
          <p className="muted">
            {mode === 'login' ? 'Sign in to search flights and manage your trips.' : 'It takes less than a minute.'}
          </p>

          <div className="segmented" role="tablist" aria-label="Sign in or register">
            <button
              type="button"
              role="tab"
              aria-selected={mode === 'login'}
              className={mode === 'login' ? 'active' : ''}
              onClick={() => switchMode('login')}
            >
              Sign in
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={mode === 'register'}
              className={mode === 'register' ? 'active' : ''}
              onClick={() => switchMode('register')}
            >
              Register
            </button>
          </div>

          <form className="form" onSubmit={submit}>
            {error && <Alert>{error}</Alert>}
            {mode === 'register' && (
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
            )}
            <label className="field">
              <span>Email</span>
              <input type="email" value={form.email} onChange={set('email')} required autoComplete="email" />
            </label>
            <label className="field">
              <span>Password</span>
              <input
                type="password"
                value={form.password}
                onChange={set('password')}
                required
                autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              />
            </label>
            {mode === 'register' && (
              <label className="field">
                <span>
                  Mobile number <em className="optional">optional</em>
                </span>
                <input type="tel" value={form.mobileNumber} onChange={set('mobileNumber')} autoComplete="tel" />
              </label>
            )}
            <button type="submit" className="btn btn-primary btn-block btn-lg" disabled={busy}>
              {busy ? <Spinner label="Please wait" /> : mode === 'login' ? 'Sign in' : 'Create account'}
            </button>
          </form>
        </div>
      </section>
    </div>
  );
}
