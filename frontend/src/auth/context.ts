import { createContext } from "react";

export type User = {
  id?: number;
  email?: string | null;
  name?: string | null;
  isAdmin?: boolean;
} | null;

export const CapCategory = [
  "best",
  "newCap",
  "cap",
  "bini",
  "sale",
] as const;

export type CapCategory = typeof CapCategory[number];

// Cap 타입 수정
export type Cap = {
  id?: number;
  name: string;
  price: number;
  color?: string;
  description?: string;
  mainImageUrl?: string;
  imageUrls?: string[];
  category?: CapCategory; // enum 대신 타입 사용
  stock?: number;
};

export const AuthCtx = createContext<{
  user: User;
  loading: boolean;
  refresh: () => void;
  setUser: (u: User) => void;
  logout?: () => Promise<void>;
}>({
  user: null,
  loading: true,
  refresh: () => {},
  setUser: () => {},
  logout: async () => {},
});