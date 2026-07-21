import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "./useAuth";
import type { JSX } from "react";


export default function ProtectedRoute({ children }: { children: JSX.Element }) {
const { user, loading } = useAuth();
const loc = useLocation();


if (loading) return <div className="h-32 flex justify-center items-center"><p>Loading...</p></div>; // or spinner
if (!user) return <Navigate to={`/login?redirect=${encodeURIComponent(loc.pathname)}`} replace />;
return children;
}