import { useEffect, useRef } from 'react';
import Icon from './Icon.jsx';

export function Spinner({ label = 'Loading' }) {
  return (
    <span className="spinner" role="status">
      <span className="visually-hidden">{label}</span>
    </span>
  );
}

export function PageLoader({ label = 'Loading' }) {
  return (
    <div className="page-loader">
      <Spinner label={label} />
      <span>{label}…</span>
    </div>
  );
}

export function Alert({ kind = 'error', children, onRetry }) {
  return (
    <div className={`alert alert-${kind}`} role={kind === 'error' ? 'alert' : 'status'}>
      <Icon name={kind === 'success' ? 'check' : 'alert'} size={18} />
      <div className="alert-body">{children}</div>
      {onRetry && (
        <button type="button" className="btn btn-ghost btn-sm" onClick={onRetry}>
          <Icon name="refresh" size={16} /> Retry
        </button>
      )}
    </div>
  );
}

export function EmptyState({ icon, title, children, action }) {
  return (
    <div className="empty">
      <div className="empty-icon">
        <Icon name={icon} size={28} />
      </div>
      <h3>{title}</h3>
      {children && <p>{children}</p>}
      {action}
    </div>
  );
}

const STATUS_TONES = {
  CONFIRMED: 'success',
  PENDING: 'warning',
  CANCELLED: 'neutral',
  FAILED: 'danger',
  Scheduled: 'success',
};

export function StatusBadge({ status }) {
  const label = status.charAt(0) + status.slice(1).toLowerCase();
  return <span className={`badge badge-${STATUS_TONES[status] || 'neutral'}`}>{label}</span>;
}

export function ConfirmDialog({ title, children, confirmLabel, danger, busy, onConfirm, onCancel }) {
  const cancelRef = useRef(null);

  useEffect(() => {
    cancelRef.current?.focus();
    const onKey = (e) => e.key === 'Escape' && !busy && onCancel();
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [busy, onCancel]);

  return (
    <div className="dialog-backdrop" onClick={() => !busy && onCancel()}>
      <div
        className="dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="dialog-title"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="dialog-title">{title}</h2>
        <div className="dialog-body">{children}</div>
        <div className="dialog-actions">
          <button ref={cancelRef} type="button" className="btn btn-ghost" onClick={onCancel} disabled={busy}>
            Keep it
          </button>
          <button
            type="button"
            className={`btn ${danger ? 'btn-danger' : 'btn-primary'}`}
            onClick={onConfirm}
            disabled={busy}
          >
            {busy ? <Spinner label="Working" /> : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
