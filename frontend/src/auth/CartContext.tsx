import { createContext, useState, useCallback } from "react";
import api from "../lib/axios";
import { useAuth } from "./useAuth";
import { API_BASE_URL } from "../config/apiBase";

export const CartContext = createContext({
  cartCount: 0,
  refreshCartCount: () => {},
});

export function CartProvider({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const [cartCount, setCartCount] = useState(0);

  const refreshCartCount = useCallback(() => {
    if (user && user.id) {
      api
        .get(`${API_BASE_URL}/api/cart/findAll?userId=${user.id}`)
        .then((res) => {
          const items = res.data;
          console.log("Cart items:", items);
          const totalCount = items.reduce(
            (sum: number, item: { quantity: number }) => sum + item.quantity,
            0
          );
          setCartCount(totalCount);
        })
        .catch((err) => {
          console.error("장바구니 카운트 조회 실패:", err);
          setCartCount(0);
        });
    } else {
      setCartCount(0);
    }
  }, [user]);

  return (
    <CartContext.Provider value={{ cartCount, refreshCartCount }}>
      {children}
    </CartContext.Provider>
  );
}