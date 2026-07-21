import { useContext } from "react";
import { AuthCtx } from "./context";

export const useAuth = () => useContext(AuthCtx);
