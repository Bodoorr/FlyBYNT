import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import Icon from './Icon.jsx';

const NAV = [
  { to: '/', label: 'Flights', icon: 'search', end: true },
  { to: '/bookings', label: 'My trips', icon: 'ticket' },
  { to: '/passengers', label: 'Passengers', icon: 'users' },
];

export default function Layout() {
  const { user, logout } = useAuth();
  return (
    <div className="app">
      <header className="topbar">
        <div className="container topbar-inner">
          <NavLink to="/" className="brand" aria-label="FlyBYNT home">
            <span className="brand-mark">
              <Icon name="plane" size={18} />
            </span>
            FlyBYNT
          </NavLink>
          <nav className="topnav" aria-label="Main">
            {NAV.map((item) => (
              <NavLink key={item.to} to={item.to} end={item.end} className="topnav-link">
                {item.label}
              </NavLink>
            ))}
          </nav>
          <div className="topbar-user">
            <span className="topbar-email" title={user?.email}>
              {user?.email}
            </span>
            <button type="button" className="btn btn-ghost btn-sm" onClick={logout}>
              <Icon name="logout" size={16} />
              <span className="hide-sm">Sign out</span>
            </button>
          </div>
        </div>
      </header>

      <main className="main">
        <Outlet />
      </main>

      <nav className="tabbar" aria-label="Main">
        {NAV.map((item) => (
          <NavLink key={item.to} to={item.to} end={item.end} className="tabbar-link">
            <Icon name={item.icon} size={22} />
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
