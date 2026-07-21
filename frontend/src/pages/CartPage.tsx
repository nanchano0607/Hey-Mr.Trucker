import { useContext, useEffect, useState } from "react";
import api from "../lib/axios";
import { useAuth } from "../auth/useAuth";
import { CartContext } from "../auth/CartContext";
import { useNavigate } from "react-router-dom";
import { API_BASE_URL } from "../config/apiBase";

const API = API_BASE_URL;

type CartItem = {
  id: number;
  quantity: number;
  productId: number;
  productName: string;
  price: number;
  mainImageUrl: string;
  size: string;
};

export default function CartPage() {
  const { user } = useAuth();
  const { refreshCartCount } = useContext(CartContext);
  const [items, setItems] = useState<CartItem[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const displayBg = `${API}/images/emptyload.webp`;

  // 장바구니 목록 불러오기
  const fetchCart = () => {
    if (!user?.id) return;
    setLoading(true);
    api
      .get(`${API}/api/cart/findAll?userId=${user.id}`)
      .then((res) => setItems(res.data))
      .catch((err) => {
        console.error("장바구니 조회 실패:", err);
        setItems([]);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchCart();
    // eslint-disable-next-line
  }, [user]);

  // 수량 증가
  const handleIncrease = (productId: number, size: string) => {
    api
      .post(`${API}/api/cart/increase`, {
        userId: user?.id,
        productId,
        size,
      })
      .then(() => {
        fetchCart();
        refreshCartCount();
      })
      .catch((err) => console.error("수량 증가 실패:", err));
  };

  // 수량 감소
  const handleDecrease = (productId: number, size: string) => {
    api
      .post(`${API}/api/cart/decrease`, {
        userId: user?.id,
        productId,
        size,
      })
      .then(() => {
        fetchCart();
        refreshCartCount();
      })
      .catch((err) => console.error("수량 감소 실패:", err));
  };

  // 아이템 삭제
  const handleDelete = (productId: number, size: string) => {
    api
      .post(`${API}/api/cart/delete`, {
        userId: user?.id,
        productId,
        size,
      })
      .then(() => fetchCart())
      .catch((err) => console.error("아이템 삭제 실패:", err));
  };

  // 총 가격과 총 개수 계산
  const totalPrice = items.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const totalCount = items.reduce((sum, item) => sum + item.quantity, 0);

  if (!user) {
    return (
      <div className="min-h-screen flex items-center justify-center px-4">
        <div className="bg-black/60 text-white px-6 py-4 rounded-xl">
          로그인이 필요합니다.
        </div>
      </div>
    );
  }

  return (
    <>
      {/* =================== Desktop (md 이상) =================== */}
      <div className="hidden md:block fixed inset-0 overflow-hidden">
        {/* 배경 이미지 - fixed로 고정 */}
        <div
          className="fixed inset-0 w-full h-full bg-cover bg-center pointer-events-none"
          style={{ backgroundImage: `url('${displayBg}')`, zIndex: 0 }}
        />

        {/* AddressPage와 동일한 메인 컨테이너 */}
        <main
          className="relative max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 h-full flex items-center justify-center"
          style={{ zIndex: 1, paddingTop: "10vh" }}
        >
          <div
            className="relative bg-[#01132c] ml-6"
            style={{
              imageRendering: "pixelated",
              clipPath: `polygon(
                0% 20px, 20px 20px, 20px 0%,
                calc(100% - 20px) 0%, calc(100% - 20px) 20px, 100% 20px,
                100% calc(100% - 20px), calc(100% - 20px) calc(100% - 20px), calc(100% - 20px) 100%,
                20px 100%, 20px calc(100% - 20px), 0% calc(100% - 20px)
              )`,
              padding: "20px",
              width: "80vw",
            }}
          >
            <div
              className="relative bg-[#03526a]"
              style={{
                imageRendering: "pixelated",
                clipPath: `polygon(
                  0% 18px, 18px 18px, 18px 0%,
                  calc(100% - 18px) 0%, calc(100% - 18px) 18px, 100% 18px,
                  100% calc(100% - 18px), calc(100% - 18px) calc(100% - 18px), calc(100% - 18px) 100%,
                  18px 100%, 18px calc(100% - 18px), 0% calc(100% - 18px)
                )`,
                padding: "48px",
              }}
            >
              <div
                className="absolute top-2 left-6 flex items-center gap-3 text-white font-bold text-3xl font-beaver"
                style={{ imageRendering: "pixelated", zIndex: 10 }}
              >
                <button
                  type="button"
                  onClick={() => navigate("/mygarage")}
                  aria-label="마이차고로 이동"
                  className="h-9 w-9 flex items-center justify-center rounded bg-white/10 hover:bg-white/20 transition-colors"
                >
                  <span className="text-2xl leading-none">←</span>
                </button>
                <span>Your Cart</span>
              </div>

              <div
                className="w-full px-4 bg-[#f2d4a7] overflow-y-auto"
                style={{
                  imageRendering: "pixelated",
                  clipPath: `polygon(
                    0% 16px, 16px 16px, 16px 0%,
                    calc(100% - 16px) 0%, calc(100% - 16px) 16px, 100% 16px,
                    100% calc(100% - 16px), calc(100% - 16px) calc(100% - 16px), calc(100% - 16px) 100%,
                    16px 100%, 16px calc(100% - 16px), 0% calc(100% - 16px)
                  )`,
                  height: "52vh",
                }}
              >
                <div className="max-w-3xl mx-auto h-full flex flex-col px-4 sm:px-6 lg:px-8 py-4">
                  {/* 상단: 내용 영역 (스크롤) */}
                  <div className="flex-1 overflow-y-auto pr-2">
                    {loading ? (
                      <p>Loading...</p>
                    ) : items.length === 0 ? (
                      <p>Your cart is empty.</p>
                    ) : (
                      <ul>
                        {items.map((item) => (
                          <li
                            key={item.id}
                            className="mb-4 border-b pb-4 flex items-center justify-between"
                          >
                            <div className="flex items-center gap-4">
                              <img
                                src={item.mainImageUrl}
                                alt={item.productName}
                                className="w-20 h-20 object-cover rounded-lg"
                              />
                              <div>
                                <strong>{item.productName}</strong>
                                <div className="text-sm text-gray-600">
                                  사이즈: {item.size}
                                </div>
                                <div>
                                  가격: {item.price.toLocaleString()} x{" "}
                                  {item.quantity} ={" "}
                                  <span className="font-bold">
                                    {(item.price * item.quantity).toLocaleString()}
                                  </span>
                                  원
                                </div>
                              </div>
                            </div>
                            <div className="flex items-center gap-2">
                              <button
                                onClick={() => handleDecrease(item.productId, item.size)}
                                className="px-2 py-1 rounded bg-slate-200 hover:bg-slate-300"
                              >
                                -
                              </button>
                              <span>{item.quantity}</span>
                              <button
                                onClick={() => handleIncrease(item.productId, item.size)}
                                className="px-2 py-1 rounded bg-slate-200 hover:bg-slate-300"
                              >
                                +
                              </button>
                              <button
                                onClick={() => handleDelete(item.productId, item.size)}
                                className="px-2 py-1 rounded bg-red-500 text-white hover:bg-red-600"
                              >
                                삭제
                              </button>
                            </div>
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>

                  {/* 하단: 합계 영역 */}
                  <div className="mt-4 p-4 bg-slate-100 rounded-lg flex flex-col md:flex-row md:justify-between md:items-center gap-3">
                    <div>
                      <span className="font-semibold">총 개수:</span> {totalCount}개
                    </div>
                    <div>
                      <span className="font-semibold">총 가격:</span>{" "}
                        {totalPrice.toLocaleString()}원
                    </div>
                    <div>
                      <button
                        className="ml-0 md:ml-4 px-6 py-2 bg-blue-700 text-white rounded font-semibold"
                        onClick={() =>
                          navigate("/buy", { state: { mode: "cart", items } })
                        }
                      >
                        구매하기
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>

      {/* =================== Mobile (md 미만) =================== */}
      <div
        className="block md:hidden min-h-screen text-white font-sans"
        style={{
          backgroundImage: `url('${displayBg}')`,
          backgroundSize: "cover",
          backgroundPosition: "center",
        }}
      >
        <div className="px-5 pb-10 space-y-3 overflow-y-auto pt-20">
          <div className="bg-black/50 rounded-xl p-4 space-y-4 mt-10">
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => navigate("/mygarage")}
                aria-label="마이차고로 이동"
                className="h-9 w-9 flex items-center justify-center rounded bg-white/10 hover:bg-white/20 transition-colors"
              >
                <span className="text-2xl leading-none">←</span>
              </button>
              <h2 className="text-xl font-bold">Your Cart</h2>
            </div>

      {loading ? (
        <div className="flex-1 flex items-center justify-center text-white/80">
          Loading...
        </div>
      ) : items.length === 0 ? (
        <div className="flex-1 flex flex-col items-center justify-center text-center text-white/80">
          <p className="mb-4">Your cart is empty.</p>
          <button
            onClick={() => navigate("/cap")}
            className="px-6 py-3 bg-white/20 text-white rounded-lg font-bold hover:bg-white/30 transition-colors border border-white/30"
          >
            Go to shop
          </button>
        </div>
      ) : (
        <div className="flex flex-col">
          <div className="overflow-y-auto pr-1 space-y-3">
            {items.map((item) => (
              <div
                key={item.id}
                className="p-3 bg-white/10 rounded-lg flex gap-3"
              >
                <img
                  src={item.mainImageUrl}
                  alt={item.productName}
                  className="w-20 h-20 object-cover rounded-lg flex-shrink-0"
                />
                <div className="flex-1 flex flex-col justify-between">
                  <div>
                    <div className="text-sm text-white/90 font-semibold">
                      {item.productName}
                    </div>
                    <div className="text-xs text-white/70">
                      Size: {item.size}
                    </div>
                    <div className="text-xs text-white/80 mt-1">
                      {item.price.toLocaleString()}원 × {item.quantity} ={" "}
                      <span className="font-bold">
                        {(item.price * item.quantity).toLocaleString()}원
                      </span>
                    </div>
                  </div>
                  <div className="mt-2 flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => handleDecrease(item.productId, item.size)}
                        className="w-7 h-7 flex items-center justify-center rounded bg-white/20 text-white text-sm"
                      >
                        -
                      </button>
                      <span className="text-sm">{item.quantity}</span>
                      <button
                        onClick={() => handleIncrease(item.productId, item.size)}
                        className="w-7 h-7 flex items-center justify-center rounded bg-white/20 text-white text-sm"
                      >
                        +
                      </button>
                    </div>
                    <button
                      onClick={() => handleDelete(item.productId, item.size)}
                      className="px-3 py-1 rounded bg-red-500/80 text-white text-xs"
                    >
                      Delete
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* 하단 고정: 합계 + 버튼들 */}
          <div className="mt-3 p-3 bg-white/10 rounded-lg space-y-2">
            <div className="flex justify-between text-sm">
              <span className="text-white/80">Total items</span>
              <span className="font-bold">{totalCount}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-white/80">Total price</span>
              <span className="font-bold">
                {totalPrice.toLocaleString()}원
              </span>
            </div>
            <button
              className="mt-3 w-full h-11 rounded-md bg-white/90 text-black font-bold active:scale-[0.98] transition-transform"
              onClick={() =>
                navigate("/buy", { state: { mode: "cart", items } })
              }
            >
              Checkout
            </button>
          </div>

        </div>
      )}
          </div>
        </div>
      </div>
    </>
  );
}
