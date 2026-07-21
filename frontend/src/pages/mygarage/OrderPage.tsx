import { useEffect, useMemo, useState } from "react";
import { bankCodes } from "../../utils/bankCodes";
import { useNavigate } from "react-router-dom";
import { API_BASE_URL } from "../../config/apiBase";

const SERVER = API_BASE_URL;

type Order = {
  id: number;
  orderId: string;         // 주문번호 (예: ORD20250131-1)
  status: string;
  receiverName: string;
  address: string;
  phone: string;
  totalPrice: number;
  orderDate: string;
  trackingNumber?: string | null;
  returnTrackingNumber?: string | null;
  returnReason?: string | null;
  returnMethod?: string | null;
  returnShippingFee?: number | null;
  confirmed: boolean;           // 구매확정 여부
  confirmedAt?: string | null;  // 구매확정 시간
  deliveredAt?: string | null;  // 배송 완료 시간
  orderItems: OrderItem[];
  originalPrice?: number;
  couponDiscount?: number;
  pointsDiscount?: number;
  totalDiscount?: number;
  finalPrice?: number;
  isVirtualAccount?: boolean;
};

type OrderItem = {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  orderPrice: number;
  subTotal: number;
  selectedSize: string;
};

export default function OrderPage() {
  const navigate = useNavigate();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<number | null>(null);
  const [banner, setBanner] = useState<{ type: "success" | "error" | "info"; text: string } | null>(null);
  const [sortMode, setSortMode] = useState<"asc" | "desc">("asc");

  /** ========== 금액/표시 유틸 ========== */
  const toNum = (v: any): number => {
    if (typeof v === "number" && Number.isFinite(v)) return v;
    const n = Number(v);
    return Number.isFinite(n) ? n : 0;
  };

  const money = (n: any) => `${toNum(n).toLocaleString()}원`;

  const calcOriginalPrice = (o: Order): number =>
    (o.orderItems ?? []).reduce((sum, it) => sum + toNum(it.orderPrice) * toNum(it.quantity), 0);

  const normalizeOrder = (o: Order): Order => {
    const original = toNum(o.originalPrice) || calcOriginalPrice(o);
    const coupon = toNum(o.couponDiscount);
    const points = toNum(o.pointsDiscount);
    const totalDiscount = toNum(o.totalDiscount) || (coupon + points);
    const finalPrice = toNum(o.finalPrice) || (original - totalDiscount);

    return {
      ...o,
      originalPrice: original,
      couponDiscount: coupon,
      pointsDiscount: points,
      totalDiscount,
      finalPrice,
      totalPrice: toNum(o.totalPrice),
    };
  };

  /** ========== 정렬 ========== */
  const getOrderTime = (o: Order) => {
    const t = new Date(o.orderDate).getTime();
    return Number.isFinite(t) ? t : 0;
  };

  const displayedOrders = useMemo(() => {
    const arr = [...orders];
    arr.sort((a, b) => (sortMode === "asc" ? getOrderTime(a) - getOrderTime(b) : getOrderTime(b) - getOrderTime(a)));
    return arr;
  }, [orders, sortMode]);

  /** ========== 스타일(스크롤바 숨김) ========== */
  useEffect(() => {
    const style = document.createElement("style");
    style.textContent = `
      .scrollbar-hide::-webkit-scrollbar { display: none; }
    `;
    document.head.appendChild(style);
    return () => {
      document.head.removeChild(style);
    };
  }, []);

  /** ========== 최초/포커스 시 주문 새로고침 ========== */
  useEffect(() => {
    fetchOrders();
  }, []);

  useEffect(() => {
    const handleFocus = () => fetchOrders();
    const handleVisibilityChange = () => {
      if (!document.hidden) fetchOrders();
    };

    window.addEventListener('focus', handleFocus);
    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      window.removeEventListener('focus', handleFocus);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, []);

  /** ========== 데이터 조회 ========== */
  const fetchOrders = async () => {
    try {
      const token = localStorage.getItem("access_token");
      const response = await fetch(`${SERVER}/api/orders`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (response.ok) {
        const data = await response.json();
        const normalized = (data as Order[]).map(normalizeOrder);
        setOrders(normalized);
      }
    } catch (error) {
      console.error("주문 조회 실패:", error);
    } finally {
      setLoading(false);
    }
  };

  /** ========== 상태 표시 ========== */
  const getStatusText = (status: string) => {
    const statusMap: Record<string, string> = {
      PAYMENT_PENDING: "가상계좌입금대기",
      PAYMENT_EXPIRED: "가상계좌입금만료",
      ORDERED: "상품 준비중",
      SHIPPED: "배송중",
      RETURN_SHIPPING: "반품 배송중",
      DELIVERED: "배송 완료",
      CANCELLED: "주문 취소",
      RETURN_REQUESTED: "반품 요청",
      RETURNED: "반품 완료",
    };
    const key = status?.toUpperCase?.() ?? status;
    return statusMap[key] || status;
  };

  const getStatusColor = (status: string) => {
    const colorMap: Record<string, string> = {
      PAYMENT_PENDING: "text-amber-600",
      PAYMENT_EXPIRED: "text-gray-600",
      ORDERED: "text-yellow-600",
      SHIPPED: "text-indigo-600",
      RETURN_SHIPPING: "text-orange-600",
      DELIVERED: "text-green-600",
      CANCELLED: "text-red-600",
      RETURN_REQUESTED: "text-orange-600",
      RETURNED: "text-gray-600",
    };
    const key = status?.toUpperCase?.() ?? status;
    return colorMap[key] || "text-gray-600";
  };

  /** ========== 액션들 ========== */
  const handleCancelOrder = async (orderPkId: number) => {
    const order = orders.find((o) => o.id === orderPkId);
    if (!order) return;

    // 1) 가상계좌 여부 확인 (백엔드 응답 키에 맞춰 수정)
    const isVA = !!(order as any).isVirtualAccount || !!(order as any).virtualAccount;

    // 2) 취소 확인
    if (!confirm("주문을 취소하시겠습니까?")) return;

    // 3) 가상계좌면 환불 계좌 입력 (모달/폼 대체)
    let refundBank = "";
    let refundAccount = "";
    let refundHolder = "";

    if (isVA) {
  // ✅ 검색 가능한 환불계좌 모달 (작고 시인성 좋게)
  const modal = document.createElement("div");
  modal.style.position = "fixed";
  modal.style.inset = "0";
  modal.style.background = "rgba(0,0,0,0.55)";
  modal.style.display = "flex";
  modal.style.alignItems = "center";
  modal.style.justifyContent = "center";
  modal.style.zIndex = "9999";
  modal.style.padding = "16px";

  const box = document.createElement("div");
  box.style.width = "min(420px, 92vw)";
  box.style.maxHeight = "80vh";
  box.style.overflow = "hidden";
  box.style.borderRadius = "14px";
  box.style.background = "rgba(10, 10, 10, 0.92)";
  box.style.border = "1px solid rgba(255,255,255,0.18)";
  box.style.boxShadow = "0 10px 30px rgba(0,0,0,0.45)";
  box.style.color = "white";
  box.style.fontFamily = "system-ui, -apple-system, Segoe UI, Roboto, sans-serif";

  box.innerHTML = `
    <div style="padding:14px 14px 10px 14px; border-bottom:1px solid rgba(255,255,255,0.12);">
      <div style="font-weight:800; font-size:16px; letter-spacing:0.2px;">가상계좌 환불 정보</div>
      <div style="margin-top:6px; font-size:12px; color:rgba(255,255,255,0.75);">
        환불받을 계좌 정보를 입력해주세요.
      </div>
    </div>

    <div style="padding:12px 14px; display:flex; flex-direction:column; gap:10px;">
      <div>
        <div style="font-size:12px; color:rgba(255,255,255,0.8); margin-bottom:6px;">은행 검색</div>
        <input id="bankSearch" placeholder="예: 국민, 신한, 카카오" 
          style="
            width:100%;
            padding:10px 12px;
            border-radius:10px;
            border:1px solid rgba(255,255,255,0.16);
            background:rgba(255,255,255,0.08);
            color:white;
            outline:none;
          " />
      </div>

      <div style="
        border:1px solid rgba(255,255,255,0.12);
        border-radius:10px;
        overflow:hidden;
      ">
        <div style="
          max-height:180px;
          overflow:auto;
          background:rgba(255,255,255,0.04);
        " id="bankList"></div>
      </div>

      <input type="hidden" id="selectedBankCode" value="" />
      <div id="selectedBankLabel" style="font-size:12px; color:rgba(255,255,255,0.75);">
        선택된 은행: 없음
      </div>

      <div>
        <div style="font-size:12px; color:rgba(255,255,255,0.8); margin-bottom:6px;">계좌번호</div>
        <input id="refundAccountInput" type="text" placeholder="숫자만"
          style="
            width:100%;
            padding:10px 12px;
            border-radius:10px;
            border:1px solid rgba(255,255,255,0.16);
            background:rgba(255,255,255,0.08);
            color:white;
            outline:none;
          " />
      </div>

      <div>
        <div style="font-size:12px; color:rgba(255,255,255,0.8); margin-bottom:6px;">예금주명</div>
        <input id="refundHolderInput" type="text" placeholder="예: 김건호"
          style="
            width:100%;
            padding:10px 12px;
            border-radius:10px;
            border:1px solid rgba(255,255,255,0.16);
            background:rgba(255,255,255,0.08);
            color:white;
            outline:none;
          " />
      </div>

      <div style="display:flex; gap:8px; justify-content:flex-end; padding-top:4px;">
        <button id="cancelBtn" type="button"
          style="
            padding:10px 14px;
            border-radius:10px;
            border:1px solid rgba(255,255,255,0.14);
            background:rgba(255,255,255,0.08);
            color:white;
            font-weight:700;
            cursor:pointer;
          ">
          닫기
        </button>
        <button id="okBtn" type="button"
          style="
            padding:10px 14px;
            border-radius:10px;
            border:1px solid rgba(255,255,255,0.14);
            background:rgba(37,99,235,0.95);
            color:white;
            font-weight:800;
            cursor:pointer;
          ">
          확인
        </button>
      </div>
    </div>
  `;

  modal.appendChild(box);
  document.body.appendChild(modal);

  // 은행 리스트 렌더 (검색 가능)
  const bankListEl = box.querySelector("#bankList") as HTMLDivElement;
  const bankSearchEl = box.querySelector("#bankSearch") as HTMLInputElement;
  const selectedCodeEl = box.querySelector("#selectedBankCode") as HTMLInputElement;
  const selectedLabelEl = box.querySelector("#selectedBankLabel") as HTMLDivElement;

  const renderBanks = (keyword: string) => {
    const q = keyword.trim().toLowerCase();
    const filtered = bankCodes.filter((b) => {
      const name = (b.name ?? "").toLowerCase();
      const code = (b.code ?? "").toLowerCase();
      return !q || name.includes(q) || code.includes(q);
    });

    bankListEl.innerHTML = filtered
      .slice(0, 80) // 너무 길면 컷
      .map(
        (b) => `
        <button type="button" data-code="${b.code}" data-name="${b.name}"
          style="
            width:100%;
            text-align:left;
            padding:10px 12px;
            border:0;
            background:transparent;
            color:white;
            cursor:pointer;
            display:flex;
            align-items:center;
            justify-content:space-between;
            border-bottom:1px solid rgba(255,255,255,0.07);
          "
        >
          <span style="font-weight:700;">${b.name}</span>
          <span style="font-size:12px; color:rgba(255,255,255,0.7);">${b.code}</span>
        </button>
      `
      )
      .join("");

    // 클릭 핸들러
    Array.from(bankListEl.querySelectorAll("button")).forEach((btn) => {
      btn.addEventListener("click", () => {
        const code = (btn as HTMLButtonElement).dataset.code || "";
        const name = (btn as HTMLButtonElement).dataset.name || "";
        selectedCodeEl.value = code;
        selectedLabelEl.textContent = `선택된 은행: ${name} (${code})`;
      });
    });
  };

  renderBanks("");

  bankSearchEl.addEventListener("input", () => {
    renderBanks(bankSearchEl.value);
  });

  const okBtn = box.querySelector("#okBtn") as HTMLButtonElement;
  const cancelBtn = box.querySelector("#cancelBtn") as HTMLButtonElement;

  const values = await new Promise<{ bank: string; account: string; holder: string } | null>((resolve) => {
    const close = () => {
      if (document.body.contains(modal)) document.body.removeChild(modal);
    };

    // 바깥 클릭 시 닫기(선택)
    modal.addEventListener("click", (e) => {
      if (e.target === modal) {
        close();
        resolve(null);
      }
    });

    cancelBtn.onclick = () => {
      close();
      resolve(null);
    };

    okBtn.onclick = () => {
      const bank = selectedCodeEl.value.trim();
      const account = (box.querySelector("#refundAccountInput") as HTMLInputElement).value.trim();
      const holder = (box.querySelector("#refundHolderInput") as HTMLInputElement).value.trim();

      if (!bank) return alert("은행을 선택해야 주문취소가 가능합니다.");
      if (!account) return alert("계좌번호를 입력해야 주문취소가 가능합니다.");
      if (!holder) return alert("예금주명을 입력해야 주문취소가 가능합니다.");

      close();
      resolve({ bank, account, holder });
    };
  });

  if (!values) return;

  refundBank = values.bank;    // ✅ 예: "004"
  refundAccount = values.account;
  refundHolder = values.holder;
}

    setActionLoading(orderPkId);

    try {
      const token = localStorage.getItem("access_token");

      const response = await fetch(`${SERVER}/api/orders/${orderPkId}/cancel`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        // ✅ 가상계좌면 환불계좌 포함해서 전송
        body: JSON.stringify(
          isVA
            ? {
                refundBank,
                refundAccount,
                refundHolder,
              }
            : {} // 가상계좌 아니면 빈 바디
        ),
      });

      if (response.ok) {
        alert("주문이 취소되었습니다.");
        fetchOrders();
      } else {
        const data = await response.json().catch(() => ({}));
        alert((data as any).error || "주문 취소에 실패했습니다.");
      }
    } catch (error) {
      console.error("주문 취소 실패:", error);
      alert("오류가 발생했습니다.");
    } finally {
      setActionLoading(null);
    }
  };

  const handleConfirmPurchase = async (orderId: number) => {
    if (!confirm("구매를 확정하시겠습니까? 구매확정 후에는 반품이 불가능합니다.")) return;

    setActionLoading(orderId);
    try {
      const token = localStorage.getItem("access_token");
      const response = await fetch(`${SERVER}/api/orders/${orderId}/confirm`, {
        method: "POST",
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });

      if (response.ok) {
        setBanner({ type: "success", text: "구매가 확정되었습니다." });
        fetchOrders();
      } else if (response.status === 401 || response.status === 403) {
        setBanner({ type: "error", text: "인증이 만료되었습니다. 다시 로그인해주세요." });
        navigate("/login");
      } else {
        const data = await response.json().catch(() => ({}));
        setBanner({ type: "error", text: (data as any).error || "구매확정에 실패했습니다." });
      }
    } catch (error) {
      console.error("구매확정 실패:", error);
      setBanner({ type: "error", text: "오류가 발생했습니다." });
    } finally {
      setActionLoading(null);
    }
  };

  /** ========== 렌더링 ========== */
  return (
    <>
      {/* 전체 고정 컨테이너 (PC : md 이상에서 표시) */}
      <div className="hidden md:block fixed inset-0 overflow-hidden">
        {/* 배경 */}
        <div
          className="fixed inset-0 w-full h-full bg-cover bg-center"
          style={{
            backgroundImage: `url('${SERVER}/images/emptyload.webp')`,
            zIndex: 0,
          }}
        />

        {/* 내용 래퍼 */}
        <div className="relative max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 h-full flex items-center justify-center" style={{ zIndex: 1, paddingTop: "10vh" }}>
          {/* 바깥 테두리 */}
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
            {/* 중간 테두리 */}
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
              {/* 좌상단 타이틀 + 뒤로가기 화살표 */}
              <div
  className="absolute top-2 left-6"
  style={{ zIndex: 10 }}
>
  <button
    type="button"
    onClick={() => navigate("/mygarage")}
    aria-label="마이차고로 이동"
    className="h-9 w-9 flex items-center justify-center rounded bg-white/10 hover:bg-white/20 transition-colors"
  >
    <span className="text-2xl leading-none">←</span>
  </button>
</div>

{/* 중앙 타이틀 */}
<div
  className="
    absolute top-2 left-1/2
    -translate-x-1/2
    text-white font-bold text-3xl font-beaver
  "
  style={{ imageRendering: "pixelated", zIndex: 10 }}
>
  Order
</div>
              {/* 안쪽 컨텐츠 */}
              <div
                className="w-full px-4 bg-[#f2d4a7] scrollbar-hide overflow-y-auto"
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
                <div className="max-w-2xl w-full mx-auto px-4 sm:px-6 lg:px-8">
                  <div className="bg-transparent p-6 rounded-lg">
                    {!loading && (
                      <div className="flex justify-end mb-3">
                        <button
                          onClick={() => setSortMode((m) => (m === "asc" ? "desc" : "asc"))}
                          className="px-3 py-1.5 bg-white/20 text-black rounded font-bold hover:bg-white/30 border border-white/30 text-sm"
                        >
                          {sortMode === "asc" ? "최신순" : "시간순"}
                        </button>
                      </div>
                    )}
                    {loading ? (
                      <p className="text-black text-base">로딩 중...</p>
                    ) : orders.length === 0 ? (
                      <div className="text-center py-12">
                        <p className="text-black text-base mb-4">주문 내역이 없습니다.</p>
                        <button
                          onClick={() => navigate("/cap")}
                          className="px-6 py-2 bg-white/20 text-black rounded-lg font-bold hover:bg-white/30 transition-colors border border-white/30"
                        >
                          쇼핑하러 가기
                        </button>
                      </div>
                    ) : (
                      <div className="h-96 overflow-y-auto pr-2 scrollbar-hide" style={{ scrollbarWidth: "none", msOverflowStyle: "none" }}>
                        <div className="space-y-3">
                          {displayedOrders.map((order) => (
                            <div
                              key={order.id}
                              className="bg-white/10 rounded-lg border border-white/20 p-4 hover:bg-white/15 transition-colors"
                            >
                              <div className="flex items-start justify-between mb-3">
                                <div>
                                  <div className="text-sm text-gray-600 mb-1">
                                    {new Date(order.orderDate).toLocaleString("ko-KR")}
                                  </div>
                                  <div className="text-base font-bold text-black">{order.orderId}</div>
                                  <div className={`text-sm mt-1 ${getStatusColor(order.status)}`}>
                                    {getStatusText(order.status)}
                                  </div>
                                </div>
                                <div className="text-right">
                                  <div className="text-lg font-bold text-red-600">
                                    {money(order.totalPrice)}
                                  </div>
                                </div>
                              </div>

                              <div className="space-y-2">
                                <div className="grid grid-cols-3 gap-2">
                                  <button
                                    onClick={() => navigate(`/order/${order.id}`)}
                                    className="w-full py-1.5 flex items-center justify-center rounded-md bg-white/20 text-black hover:bg-white/30 transition-colors font-medium text-sm"
                                  >
                                    상세보기
                                  </button>

                                  <button
                                    onClick={() => handleConfirmPurchase(order.id)}
                                    disabled={actionLoading === order.id || !(order.status === "SHIPPED" || order.status === "DELIVERED") || order.confirmed}
                                    className={`w-full py-1.5 rounded-md transition-colors text-sm ${
                                      (order.status === "SHIPPED" || order.status === "DELIVERED") && !order.confirmed
                                        ? "bg-green-500/80 text-white hover:bg-green-600 disabled:opacity-50 disabled:cursor-not-allowed"
                                        : "bg-gray-300 text-gray-600 cursor-not-allowed"
                                    }`}
                                  >
                                    {order.confirmed ? "구매확정 완료" : actionLoading === order.id ? "처리중..." : "구매확정"}
                                  </button>

                                  <button
                                    onClick={() => handleCancelOrder(order.id)}
                                    disabled={actionLoading === order.id || order.status !== "ORDERED"}
                                    title={order.status === "PREPARING_SHIPMENT" ? "배송준비중에는 주문취소가 불가능합니다." : ""}
                                    className={`w-full py-1.5 rounded-md transition-colors text-sm ${
                                      order.status === "ORDERED"
                                        ? "bg-red-500/80 text-white hover:bg-red-600 disabled:opacity-50 disabled:cursor-not-allowed"
                                        : "bg-gray-300 text-gray-600 cursor-not-allowed"
                                    }`}
                                  >
                                    {actionLoading === order.id ? "처리중..." : "주문취소"}
                                  </button>
                                </div>
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              </div>
              {/* 안쪽 컨텐츠 끝 */}
            </div>
            {/* 중간 테두리 끝 */}
          </div>
          {/* 바깥 테두리 끝 */}
        </div>
        {/* 내용 래퍼 끝 */}
      </div>
      {/* 전체 고정 컨테이너 끝 */}

      {/* ================= 모바일 (md 미만) 버전: 단순화된 스택 리스트 ================= */}
     <div
      className="block md:hidden min-h-screen text-white font-sans"
      style={{
        backgroundImage: `url('${SERVER}/images/emptyload.webp')`,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
      }}
     >
      {/* 네비게이션 높이만큼 위 여백 주고, 가운데 정렬 */}
      <div className="w-full h-full flex justify-center items-start pt-20 px-5">
        {/* 🔒 여기 컨테이너는 고정, 내부만 스크롤 */}
        <div className="w-full max-w-md bg-black/50 rounded-xl p-3 mt-4 flex flex-col max-h-[calc(100vh-120px)] mt-10">
          
          {/* 타이틀 + 뒤로가기 버튼 */}
          {/* 타이틀 + 뒤로가기 버튼 */}
<div className="flex items-center gap-3 mb-4 pb-3 border-b border-white/20">
  <button
    type="button"
    onClick={() => navigate("/mygarage")}
    aria-label="마이차고로 이동"
    className="h-9 w-9 flex items-center justify-center rounded bg-white/10 hover:bg-white/20 transition-colors"
  >
    <span className="text-2xl leading-none">←</span>
  </button>

  <h2 className="text-xl font-bold text-white">
    Order
  </h2>
</div>

          
          {/* 안쪽 내용 전체를 스크롤 영역으로 */}
          <div className="space-y-3 overflow-y-auto scrollbar-hide pr-1">
            {loading ? (
              <p className="text-white/80 p-4">로딩 중...</p>
            ) : displayedOrders.length === 0 ? (
              <div className="text-center py-8">
                <p className="text-white text-base mb-4">주문 내역이 없습니다.</p>
                <button
                  onClick={() => navigate(-1)}
                  className="px-6 py-3 bg-white/20 text-white rounded-lg font-bold hover:bg-white/30 transition-colors border border-white/30"
                >
                  뒤로가기
                </button>
              </div>
            ) : (
              <div className="space-y-3">
                {displayedOrders.map((order) => (
                  <div key={order.id} className="w-full bg-transparent text-white rounded-xl p-4">
                    <div className="flex items-start justify-between">
                      <div>
                        <div className="text-sm text-white/90">
                          {new Date(order.orderDate).toLocaleString("ko-KR")}
                        </div>
                        <div className="text-base font-bold">{order.orderId}</div>
                        <div className={`text-sm mt-1 ${getStatusColor(order.status)}`}>
                          {getStatusText(order.status)}
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="text-lg font-bold text-red-400">
                          {money(order.finalPrice || order.totalPrice)}
                        </div>
                      </div>
                    </div>

                    <div className="mt-3 space-y-2">
                      <div className="grid grid-cols-3 gap-2">
                        <button
                          onClick={() => navigate(`/order/${order.id}`)}
                          className="w-full h-10 flex items-center justify-center rounded-md bg-white/10 text-white hover:bg-white/20 transition-colors text-sm"
                        >
                          상세보기
                        </button>

                        <button
                          onClick={() => handleConfirmPurchase(order.id)}
                          disabled={actionLoading === order.id || !(order.status === "SHIPPED" || order.status === "DELIVERED") || order.confirmed}
                          className={`h-10 w-full rounded-md transition-colors text-sm ${
                            (order.status === "SHIPPED" || order.status === "DELIVERED") && !order.confirmed
                              ? "bg-green-500/80 text-white hover:bg-green-600 disabled:opacity-50 disabled:cursor-not-allowed"
                              : "bg-gray-600 text-gray-400 cursor-not-allowed"
                          }`}
                        >
                          {order.confirmed ? "구매확정 완료" : actionLoading === order.id ? "처리중..." : "구매확정"}
                        </button>

                        <button
                          onClick={() => handleCancelOrder(order.id)}
                          disabled={actionLoading === order.id || order.status !== "ORDERED"}
                          className={`h-10 w-full rounded-md transition-colors text-sm ${
                            order.status === "ORDERED"
                              ? "bg-red-500/80 text-white hover:bg-red-600 disabled:opacity-50 disabled:cursor-not-allowed"
                              : "bg-gray-600 text-gray-400 cursor-not-allowed"
                          }`}
                        >
                          {actionLoading === order.id ? "처리중..." : "주문취소"}
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>

      {/* 안내 배너 */}
      {banner && (
        <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50">
          <div
            className={`px-4 py-2 rounded shadow text-white ${
              banner.type === "success" ? "bg-green-600" : banner.type === "info" ? "bg-blue-600" : "bg-red-600"
            }`}
          >
            {banner.text}
            <button className="ml-3 underline text-white/90" onClick={() => setBanner(null)}>
              닫기
            </button>
          </div>
        </div>
      )}
    </>
  );
}
