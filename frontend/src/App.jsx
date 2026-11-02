import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { useAuth } from './auth.jsx';
import Layout from './components/Layout.jsx';
import AuthPage from './pages/AuthPage.jsx';
import BookPage from './pages/BookPage.jsx';
import BookingsPage from './pages/BookingsPage.jsx';
import PassengersPage from './pages/PassengersPage.jsx';
import SearchPage from './pages/SearchPage.jsx';

function RequireAuth({ children }) {
  const { token } = useAuth();
  const location = useLocation();
  if (!token) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<AuthPage />} />
      <Route
        element={
          <RequireAuth>
            <Layout />
          </RequireAuth>
        }
      >
        <Route index element={<SearchPage />} />
        <Route path="book/:flightId" element={<BookPage />} />
        <Route path="bookings" element={<BookingsPage />} />
        <Route path="passengers" element={<PassengersPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
