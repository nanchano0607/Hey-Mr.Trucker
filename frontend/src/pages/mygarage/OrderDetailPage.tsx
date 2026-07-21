import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { API_BASE_URL } from "../../config/apiBase";

const SERVER = API_BASE_URL;

type Order = {
  id: number;
  orderId: string;
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
  confirmed: boolean;
  confirmedAt?: string | null;
  deliveredAt?: string | null;
  orderItems: OrderItem[];
  originalPrice?: number;
  couponDiscount?: number;
  pointsDiscount?: number;
  totalDiscount?: number;
};

type OrderItem = {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  orderPrice: number;
  subTotal: number;
  selectedSize: string;
  productType?: string;
};

export default function OrderDetailPage() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [showReturnModal, setShowReturnModal] = useState(false);
  const [banner, setBanner] = useState<{ type: "success" | "error" | "info"; text: string } | null>(null);
  const [returnMethod, setReturnMethod] = useState<"PICKUP" | null>(null);
  const [returnReason, setReturnReason] = useState<"DEFECT" | "CUSTOM" | null>(null);
  const [customReturnReason, setCustomReturnReason] = useState<string>("");
  const [reviewStatus, setReviewStatus] = useState<{ [key: string]: boolean }>({});
  const [productImages, setProductImages] = useState<{ [key: number]: string }>({});

  /** ========== 금액/표시 유틸 ========== */
  const SHIPPING_FEE = 3500;
  const FREE_SHIPPING_THRESHOLD = 70000;

  const toNum = (v: any): number => {
    if (typeof v === "number" && Number.isFinite(v)) return v;
    const n = Number(v);
    return Number.isFinite(n) ? n : 0;
  };

  const money = (n: any) => `${toNum(n).toLocaleString()}원`;

  // 배송비 계산: 상품금액이 7만원 미만이면 3500원 추가
  const calculateDisplayPrice = (productAmount: any): number => {
    const amount = toNum(productAmount);
    return amount >= FREE_SHIPPING_THRESHOLD 
      ? amount 
      : amount + SHIPPING_FEE;
  };

  const calcOriginalPrice = (o: Order): number =>
    (o.orderItems ?? []).reduce((sum, it) => sum + toNum(it.orderPrice) * toNum(it.quantity), 0);

  const normalizeOrder = (o: Order): Order => {
    const original = toNum(o.originalPrice) || calcOriginalPrice(o);
    const coupon = toNum(o.couponDiscount);
    const points = toNum(o.pointsDiscount);
    const totalDiscount = toNum(o.totalDiscount) || (coupon + points);

    return {
      ...o,
      originalPrice: original,
      couponDiscount: coupon,
      pointsDiscount: points,
      totalDiscount,
      totalPrice: toNum(o.totalPrice),
    };
  };

  /** ========== 데이터 조회 ========== */
  useEffect(() => {
    fetchOrderDetail();
  }, [id]);

  const fetchOrderDetail = async () => {
    if (!id) return;

    try {
      const token = localStorage.getItem("access_token");
      const response = await fetch(`${SERVER}/api/orders/${id}`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      
      if (response.ok) {
        const data = await response.json();
        const normalized = normalizeOrder(data as Order);
        console.log('[주문 정보]', normalized);
        console.log('[주문 상태]', `status: ${normalized.status}, confirmed: ${normalized.confirmed}`);
        setOrder(normalized);
        await checkReviewStatus(normalized);
        await loadProductImages(normalized);
      } else if (response.status === 404) {
        setBanner({ type: "error", text: "주문을 찾을 수 없습니다." });
      } else if (response.status === 401 || response.status === 403) {
        setBanner({ type: "error", text: "로그인이 필요합니다." });
        navigate("/login");
      }
    } catch (error) {
      console.error("주문 조회 실패:", error);
      setBanner({ type: "error", text: "주문 정보를 불러오는데 실패했습니다." });
    } finally {
      setLoading(false);
    }
  };

  const checkReviewStatus = async (orderData: Order) => {
    const token = localStorage.getItem("access_token");
    const status: { [key: string]: boolean } = {};

    for (const item of orderData.orderItems) {
      const key = `${item.id}`;
      try {
        const response = await fetch(`${SERVER}/api/reviews/check?orderId=${orderData.id}&productId=${item.productId}`, {
          headers: token ? { Authorization: `Bearer ${token}` } : {},
        });
        if (response.ok) {
          const data = await response.json();
          console.log(`[리뷰 체크] OrderItem ${item.id}, Product ${item.productId}:`, data);
          status[key] = !!data.alreadyReviewed;
        } else {
          console.log(`[리뷰 체크 실패] OrderItem ${item.id}, Status: ${response.status}`);
          status[key] = false;
        }
      } catch (error) {
        console.error(`[리뷰 체크 에러] OrderItem ${item.id}:`, error);
        status[key] = false;
      }
    }
    console.log('[리뷰 상태 최종]', status);
    setReviewStatus(status);
  };

  const loadProductImages = async (orderData: Order) => {
    const images: { [key: number]: string } = {};
    
    for (const item of orderData.orderItems) {
      try {
        const productType = item.productType?.toLowerCase() || 'cap';
        const response = await fetch(`${SERVER}/api/${productType}/getImages/${item.productId}`);
        
        if (response.ok) {
          const filenames: string[] = await response.json();
          if (filenames.length > 0) {
            images[item.productId] = `${SERVER}/images/${filenames[0]}`;
          }
        }
      } catch (error) {
        console.error(`Failed to load image for product ${item.productId}:`, error);
      }
    }
    
    setProductImages(images);
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
  const handleCancelOrder = async () => {
    if (!order || !confirm("주문을 취소하시겠습니까?")) return;

    setActionLoading(true);
    try {
      const token = localStorage.getItem("access_token");
      const response = await fetch(`${SERVER}/api/orders/${order.id}/cancel`, {
        method: "POST",
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });

      if (response.ok) {
        setBanner({ type: "success", text: "주문이 취소되었습니다." });
        fetchOrderDetail();
      } else {
        const data = await response.json().catch(() => ({}));
        setBanner({ type: "error", text: (data as any).error || "주문 취소에 실패했습니다." });
      }
    } catch (error) {
      console.error("주문 취소 실패:", error);
      setBanner({ type: "error", text: "오류가 발생했습니다." });
    } finally {
      setActionLoading(false);
    }
  };

  const openReturnModal = () => {
    setShowReturnModal(true);
    setReturnMethod("PICKUP"); // 회수 요청만 가능
    setReturnReason(null);
    setCustomReturnReason("");
  };

  const submitReturnRequest = async () => {
    if (!order) return;
    if (!returnMethod) {
      setBanner({ type: "error", text: "반품 방법을 선택해주세요." });
      return;
    }
    if (!returnReason) {
      setBanner({ type: "error", text: "반품 사유를 선택해주세요." });
      return;
    }
    if (returnReason === "CUSTOM" && !customReturnReason.trim()) {
      setBanner({ type: "error", text: "반품 사유를 입력해주세요." });
      return;
    }

    setActionLoading(true);
    try {
      const token = localStorage.getItem("access_token");
      if (!token) {
        setBanner({ type: "error", text: "로그인이 필요합니다." });
        navigate("/login");
        return;
      }

      const returnShippingFee = 0; // 회수 요청만 가능하므로 항상 배송비 없음

      const response = await fetch(`${SERVER}/api/orders/${order.id}/return`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          returnReason: returnReason === "CUSTOM" ? customReturnReason : returnReason,
          returnMethod,
          returnShippingFee,
        }),
      });

      if (response.ok) {
        setBanner({ type: "success", text: "반품이 요청되었습니다." });
        fetchOrderDetail();
      } else if (response.status === 404) {
        setBanner({ type: "error", text: "주문을 찾을 수 없습니다." });
      } else if (response.status === 401 || response.status === 403) {
        setBanner({ type: "error", text: "인증이 만료되었습니다. 다시 로그인해주세요." });
        navigate("/login");
      } else {
        const data = await response.json().catch(() => ({}));
        setBanner({ type: "error", text: (data as any).error || "반품 요청에 실패했습니다." });
      }
    } catch (error) {
      console.error("반품 요청 실패:", error);
      setBanner({ type: "error", text: "오류가 발생했습니다." });
    } finally {
      setActionLoading(false);
      setShowReturnModal(false);
      setReturnMethod(null);
      setReturnReason(null);
      setCustomReturnReason("");
    }
  };

  const handleCancelReturn = async () => {
    if (!order || order.status !== "RETURN_REQUESTED") return;
    if (!confirm("반품 요청을 취소하시겠습니까?")) return;

    setActionLoading(true);
    try {
      const token = localStorage.getItem("access_token");
      if (!token) {
        setBanner({ type: "error", text: "로그인이 필요합니다." });
        navigate("/login");
        return;
      }

      const response = await fetch(`${SERVER}/api/orders/${order.id}/cancel-return`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      });

      if (response.ok) {
        setBanner({ type: "success", text: "반품 요청이 취소되었습니다." });
        fetchOrderDetail();
      } else if (response.status === 401 || response.status === 403) {
        setBanner({ type: "error", text: "인증이 만료되었습니다. 다시 로그인해주세요." });
        navigate("/login");
      } else if (response.status === 404) {
        setBanner({ type: "error", text: "주문을 찾을 수 없습니다." });
      } else {
        const data = await response.json().catch(() => ({}));
        setBanner({ type: "error", text: (data as any).error || "반품 취소에 실패했습니다." });
      }
    } catch (error) {
      console.error("반품 취소 실패:", error);
      setBanner({ type: "error", text: "오류가 발생했습니다." });
    } finally {
      setActionLoading(false);
    }
  };

  const handleConfirmPurchase = async () => {
    if (!order || !confirm("구매를 확정하시겠습니까? 구매확정 후에는 반품이 불가능합니다.")) return;

    setActionLoading(true);
    try {
      const token = localStorage.getItem("access_token");
      const response = await fetch(`${SERVER}/api/orders/${order.id}/confirm`, {
        method: "POST",
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });

      if (response.ok) {
        setBanner({ type: "success", text: "구매가 확정되었습니다." });
        fetchOrderDetail();
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
      setActionLoading(false);
    }
  };

  const handleWriteReview = (productId: number) => {
    if (!order) return;
    navigate(`/review/write?productId=${productId}&orderId=${order.id}`);
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center text-white">
        로딩 중...
      </div>
    );
  }

  if (!order) {
    return (
      <div className="min-h-screen flex items-center justify-center text-white">
        <div className="text-center">
          <p className="mb-4">주문을 찾을 수 없습니다.</p>
          <button
            onClick={() => navigate("/order")}
            className="px-6 py-2 bg-white/20 text-white rounded-lg font-bold hover:bg-white/30 transition-colors border border-white/30"
          >
            주문 목록으로
          </button>
        </div>
      </div>
    );
  }

  /** ========== 렌더링 ========== */
  return (
    <>
      {/* PC 버전 */}
      <div className="hidden md:block fixed inset-0 overflow-hidden">
        <div
          className="fixed inset-0 w-full h-full bg-cover bg-center"
          style={{
            backgroundImage: `url('${SERVER}/images/emptyload.webp')`,
            zIndex: 0,
          }}
        />

        <div className="relative max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 h-full flex items-center justify-center" style={{ zIndex: 1, paddingTop: "10vh" }}>
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
                  onClick={() => navigate("/order")}
                  aria-label="주문 목록으로 이동"
                  className="h-9 w-9 flex items-center justify-center rounded bg-white/10 hover:bg-white/20 transition-colors"
                >
                  <span className="text-2xl leading-none">←</span>
                </button>
                <span>Order</span>
              </div>

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
                    {/* 주문 기본 정보 */}
                    <div className="bg-white/10 rounded-lg border border-white/20 p-4 mb-4">
                      <div className="flex items-start justify-between mb-3">
                        <div>
                          <div className="text-sm text-gray-600 mb-1">
                            {new Date(order.orderDate).toLocaleString("ko-KR")}
                          </div>
                          <div className="text-lg font-bold text-black">{order.orderId}</div>
                           <div className={`text-sm mt-1 ${getStatusColor(order.status)}`}>
                             {getStatusText(order.status)}
                          </div>
                        </div>
                        <div className="text-right">
                          <div className="text-xl font-bold text-red-600">
                            {money(order.totalPrice)}
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* 배송지 정보 */}
                    <div className="bg-white/10 rounded-lg border border-white/20 p-4 mb-4">
                      <h3 className="text-base font-bold text-black mb-3">배송지 정보</h3>
                      <div className="space-y-2 text-sm text-black">
                        <div><span className="font-semibold">수령인:</span> {order.receiverName}</div>
                        <div><span className="font-semibold">연락처:</span> {order.phone}</div>
                        <div><span className="font-semibold">주소:</span> {order.address}</div>
                        {order.trackingNumber && (
                          <div>
                            <span className="font-semibold">송장번호:</span>{" "}
                            <a 
                              href="https://www.cjlogistics.com/ko/tool/parcel/tracking" 
                              target="_blank" 
                              rel="noopener noreferrer"
                              className="font-mono text-blue-600 hover:text-blue-800 underline"
                            >
                              {order.trackingNumber}
                            </a>
                          </div>
                        )}
                        {order.returnTrackingNumber && (
                          <div>
                            <span className="font-semibold">반품 송장번호:</span>{" "}
                            <a 
                              href="https://www.cjlogistics.com/ko/tool/parcel/tracking" 
                              target="_blank" 
                              rel="noopener noreferrer"
                              className="font-mono text-blue-600 hover:text-blue-800 underline"
                            >
                              {order.returnTrackingNumber}
                            </a>
                          </div>
                        )}
                      </div>
                    </div>

                    {/* 주문 상품 */}
                    <div className="bg-white/10 rounded-lg border border-white/20 p-4 mb-4">
                      <h3 className="text-base font-bold text-black mb-3">주문 상품</h3>
                      <div className="space-y-3">
                        {order.orderItems.map((item) => {
                          const reviewKey = `${item.id}`;
                          const hasReview = reviewStatus[reviewKey];
                          const imageUrl = productImages[item.productId];

                          return (
                            <div key={item.id} className="flex items-start gap-3 border-b border-white/10 pb-3 last:border-0">
                              {imageUrl && (
                                <img
                                  src={imageUrl}
                                  alt={item.productName}
                                  className="w-16 h-16 object-cover rounded border border-white/20 flex-shrink-0"
                                  onError={(e) => {
                                    e.currentTarget.style.display = 'none';
                                  }}
                                />
                              )}
                              <div className="flex-1">
                                <div className="text-sm font-semibold text-black">{item.productName}</div>
                                <div className="text-xs text-gray-600 mt-1">
                                  사이즈: {item.selectedSize} | 수량: {item.quantity}개
                                </div>
                                <div className="text-sm text-black mt-1">
                                  {money(item.orderPrice)} × {item.quantity} = {money(item.subTotal)}
                                </div>
                              </div>
                              {order.status === "DELIVERED" && (
                                <button
                                  onClick={() => handleWriteReview(item.productId)}
                                  disabled={hasReview}
                                  className={`ml-3 px-3 py-1.5 text-xs rounded ${
                                    hasReview
                                      ? "bg-gray-400 text-gray-600 cursor-not-allowed"
                                      : "bg-blue-500/80 text-white hover:bg-blue-600"
                                  }`}
                                >
                                  {hasReview ? "리뷰 작성완료" : "리뷰 작성"}
                                </button>
                              )}
                            </div>
                          );
                        })}
                      </div>
                    </div>

                    {/* 결제 정보 */}
                    <div className="bg-white/10 rounded-lg border border-white/20 p-4 mb-4">
                      <h3 className="text-base font-bold text-black mb-3">결제 정보</h3>
                      <div className="space-y-2 text-sm text-black">
                        <div className="flex justify-between">
                          <span>상품 금액</span>
                          <span>{money(calculateDisplayPrice(order.originalPrice))}</span>
                        </div>
                        {(order.couponDiscount ?? 0) > 0 && (
                          <div className="flex justify-between text-red-600">
                            <span>쿠폰 할인</span>
                            <span>-{money(order.couponDiscount)}</span>
                          </div>
                        )}
                        {(order.pointsDiscount ?? 0) > 0 && (
                          <div className="flex justify-between text-red-600">
                            <span>포인트 사용</span>
                            <span>-{money(order.pointsDiscount)}</span>
                          </div>
                        )}
                        <div className="flex justify-between font-bold text-base pt-2 border-t border-white/20">
                          <span>최종 결제 금액</span>
                          <span className="text-red-600">{money(order.totalPrice)}</span>
                        </div>
                      </div>
                    </div>

                    {/* 반품 정보 (반품 요청/완료 시) */}
                    {(order.status === "RETURN_REQUESTED" || order.status === "RETURNED" || order.status === "RETURN_SHIPPING") && (
                      <div className="bg-white/10 rounded-lg border border-white/20 p-4 mb-4">
                        <h3 className="text-base font-bold text-black mb-3">반품 정보</h3>
                        <div className="space-y-2 text-sm text-black">
                          {order.returnReason && (
                            <div>
                              <span className="font-semibold">반품 사유:</span>{" "}
                              {order.returnReason === "DEFECT" ? "제품 하자" : "단순 변심"}
                            </div>
                          )}
                          {order.returnMethod && (
                            <div>
                              <span className="font-semibold">반품 방법:</span>{" "}
                              {order.returnMethod === "SELF" ? "직접 반품" : "회수 요청"}
                            </div>
                          )}
                          {order.returnShippingFee !== null && order.returnShippingFee !== undefined && (
                            <div>
                              <span className="font-semibold">반품 배송비:</span> {money(order.returnShippingFee)}
                            </div>
                          )}
                        </div>
                      </div>
                    )}

                    {/* 액션 버튼 */}
                    <div className="space-y-2">
                      {order.status === "ORDERED" && (
                        <button
                          onClick={handleCancelOrder}
                          disabled={actionLoading}
                          className="w-full py-1.5 rounded-md bg-red-500/80 text-white hover:bg-red-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors text-sm"
                        >
                          {actionLoading ? "처리중..." : "주문 취소"}
                        </button>
                      )}

                      {(order.status === "SHIPPED" || order.status === "DELIVERED") && (
                        <>
                          {order.status === "DELIVERED" ? (
                            <div className="grid grid-cols-2 gap-2">
                              <button
                                onClick={handleConfirmPurchase}
                                disabled={actionLoading || order.confirmed}
                                className={`py-2 rounded-md transition-colors ${
                                  order.confirmed
                                    ? "bg-gray-400 text-gray-600 cursor-not-allowed"
                                    : "bg-green-500/80 text-white hover:bg-green-600 disabled:opacity-50 disabled:cursor-not-allowed"
                                }`}
                              >
                                {order.confirmed ? "구매확정 완료" : actionLoading ? "처리중..." : "구매확정"}
                              </button>

                              <button
                                onClick={openReturnModal}
                                disabled={actionLoading || order.confirmed}
                                className={`py-2 rounded-md transition-colors ${
                                  order.confirmed
                                    ? "bg-gray-400 text-gray-600 cursor-not-allowed"
                                    : "bg-orange-500/80 text-white hover:bg-orange-600 disabled:opacity-50 disabled:cursor-not-allowed"
                                }`}
                              >
                                {order.confirmed ? "반품 불가" : actionLoading ? "처리중..." : "반품 요청"}
                              </button>
                            </div>
                          ) : (
                            <button
                              onClick={handleConfirmPurchase}
                              disabled={actionLoading || order.confirmed}
                              className={`w-full py-1.5 rounded-md transition-colors text-sm ${
                                order.confirmed
                                  ? "bg-gray-400 text-gray-600 cursor-not-allowed"
                                  : "bg-green-500/80 text-white hover:bg-green-600 disabled:opacity-50 disabled:cursor-not-allowed"
                              }`}
                            >
                              {order.confirmed ? "구매확정 완료" : actionLoading ? "처리중..." : "구매확정"}
                            </button>
                          )}
                        </>
                      )}
                      {order.status === "RETURN_REQUESTED" && (
                        <button
                          onClick={handleCancelReturn}
                          disabled={actionLoading}
                          className="w-full py-1.5 rounded-md bg-red-600/80 text-white hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors text-sm"
                        >
                          {actionLoading ? "처리중..." : "반품 요청 취소"}
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 모바일 버전 */}
      <div
        className="block md:hidden min-h-screen text-white font-sans"
        style={{
          backgroundImage: `url('${SERVER}/images/emptyload.webp')`,
          backgroundSize: 'cover',
          backgroundPosition: 'center',
        }}
      >
        <div className="w-full h-full flex justify-center items-start pt-20 px-5">
          <div className="w-full max-w-md bg-black/50 rounded-xl p-4 mt-4 flex flex-col max-h-[calc(100vh-120px)] mt-10">
            <div className="flex items-center gap-3 pb-2">
              <button
                type="button"
                onClick={() => navigate("/order")}
                aria-label="주문 목록으로 이동"
                className="h-9 w-9 flex items-center justify-center rounded bg-white/10 hover:bg-white/20 transition-colors"
              >
                <span className="text-2xl leading-none">←</span>
              </button>
              <div className="text-xl font-bold">Order</div>
            </div>

            <div className="space-y-3 overflow-y-auto scrollbar-hide pr-1">
              {/* 주문 기본 정보 */}
              <div className="bg-transparent border border-white/20 rounded-lg p-4">
                <div className="flex items-start justify-between mb-3">
                  <div>
                    <div className="text-sm text-white/90">
                      {new Date(order.orderDate).toLocaleString("ko-KR")}
                    </div>
                    <div className="text-base font-bold">{order.orderId}</div>
                    <div className={`text-sm mt-1 ${order.confirmed ? "text-purple-400" : getStatusColor(order.status)}`}>
                        {getStatusText(order.status)}
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="text-lg font-bold text-red-400">
                      {money(order.totalPrice)}
                    </div>
                  </div>
                </div>
              </div>

              {/* 배송지 정보 */}
              <div className="bg-transparent border border-white/20 rounded-lg p-4">
                <h3 className="text-base font-bold mb-3">배송지 정보</h3>
                <div className="space-y-2 text-sm text-white/90">
                  <div><span className="font-semibold">수령인:</span> {order.receiverName}</div>
                  <div><span className="font-semibold">연락처:</span> {order.phone}</div>
                  <div><span className="font-semibold">주소:</span> {order.address}</div>
                  {order.trackingNumber && (
                    <div>
                      <span className="font-semibold">송장번호:</span>{" "}
                      <span className="font-mono">{order.trackingNumber}</span>
                    </div>
                  )}
                  {order.returnTrackingNumber && (
                    <div><span className="font-semibold">반품 송장번호:</span> {order.returnTrackingNumber}</div>
                  )}
                </div>
              </div>

              {/* 주문 상품 */}
              <div className="bg-transparent border border-white/20 rounded-lg p-4">
                <h3 className="text-base font-bold mb-3">주문 상품</h3>
                <div className="space-y-3">
                  {order.orderItems.map((item) => {
                    const reviewKey = `${item.id}`;
                    const hasReview = reviewStatus[reviewKey];
                    const imageUrl = productImages[item.productId];

                    return (
                      <div key={item.id} className="border-b border-white/10 pb-3 last:border-0">
                        <div className="flex items-start gap-3">
                          {imageUrl && (
                            <img
                              src={imageUrl}
                              alt={item.productName}
                              className="w-16 h-16 object-cover rounded border border-white/20 flex-shrink-0"
                              onError={(e) => {
                                e.currentTarget.style.display = 'none';
                              }}
                            />
                          )}
                          <div className="flex-1">
                            <div className="text-sm font-semibold">{item.productName}</div>
                            <div className="text-xs text-white/70 mt-1">
                              사이즈: {item.selectedSize} | 수량: {item.quantity}개
                            </div>
                            <div className="text-sm mt-1">
                              {money(item.orderPrice)} × {item.quantity} = {money(item.subTotal)}
                            </div>
                          </div>
                        </div>
                        {order.status === "DELIVERED" && (
                          <button
                            onClick={() => handleWriteReview(item.productId)}
                            disabled={hasReview}
                            className={`w-full mt-2 px-3 py-2 text-xs rounded ${
                              hasReview
                                ? "bg-gray-600 text-gray-400 cursor-not-allowed"
                                : "bg-blue-500 text-white hover:bg-blue-600"
                            }`}
                          >
                            {hasReview ? "리뷰 작성완료" : "리뷰 작성"}
                          </button>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* 결제 정보 */}
              <div className="bg-transparent border border-white/20 rounded-lg p-4">
                <h3 className="text-base font-bold mb-3">결제 정보</h3>
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span>상품 금액</span>
                    <span>{money(calculateDisplayPrice(order.originalPrice))}</span>
                  </div>
                  {(order.couponDiscount ?? 0) > 0 && (
                    <div className="flex justify-between text-red-400">
                      <span>쿠폰 할인</span>
                      <span>-{money(order.couponDiscount)}</span>
                    </div>
                  )}
                  {(order.pointsDiscount ?? 0) > 0 && (
                    <div className="flex justify-between text-red-400">
                      <span>포인트 사용</span>
                      <span>-{money(order.pointsDiscount)}</span>
                    </div>
                  )}
                  <div className="flex justify-between font-bold text-base pt-2 border-t border-white/20">
                    <span>최종 결제 금액</span>
                    <span className="text-red-400">{money(order.totalPrice)}</span>
                  </div>
                </div>
              </div>

              {/* 반품 정보 */}
              {(order.status === "RETURN_REQUESTED" || order.status === "RETURNED" || order.status === "RETURN_SHIPPING") && (
                <div className="bg-transparent border border-white/20 rounded-lg p-4">
                  <h3 className="text-base font-bold mb-3">반품 정보</h3>
                  <div className="space-y-2 text-sm text-white/90">
                    {order.returnReason && (
                      <div>
                        <span className="font-semibold">반품 사유:</span>{" "}
                        {order.returnReason === "DEFECT" ? "제품 하자" : "단순 변심"}
                      </div>
                    )}
                    {order.returnMethod && (
                      <div>
                        <span className="font-semibold">반품 방법:</span>{" "}
                        {order.returnMethod === "SELF" ? "직접 반품" : "회수 요청"}
                      </div>
                    )}
                    {order.returnShippingFee !== null && order.returnShippingFee !== undefined && (
                      <div>
                        <span className="font-semibold">반품 배송비:</span> {money(order.returnShippingFee)}
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* 액션 버튼 */}
              <div className="space-y-2">
                {order.status === "ORDERED" && (
                  <button
                    onClick={handleCancelOrder}
                    disabled={actionLoading}
                    className="w-full py-2 rounded-md bg-red-500/80 text-white hover:bg-red-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors text-sm"
                  >
                    {actionLoading ? "처리중..." : "주문 취소"}
                  </button>
                )}

                {order.status === "PREPARING_SHIPMENT" && (
                  <div className="w-full py-2 rounded-md bg-gray-400 text-white text-center text-sm opacity-50 cursor-not-allowed">
                    배송준비중 (취소 불가)
                  </div>
                )}

                {(order.status === "SHIPPED" || order.status === "DELIVERED") && (
                  <>
                    {order.status === "DELIVERED" ? (
                      <div className="grid grid-cols-2 gap-2">
                        <button
                          onClick={handleConfirmPurchase}
                          disabled={actionLoading || order.confirmed}
                          className={`py-3 rounded-md transition-colors ${
                            order.confirmed
                              ? "bg-gray-600 text-gray-400 cursor-not-allowed"
                              : "bg-green-500/80 text-white hover:bg-green-600 disabled:opacity-50 disabled:cursor-not-allowed"
                          }`}
                        >
                          {order.confirmed ? "구매확정 완료" : actionLoading ? "처리중..." : "구매확정"}
                        </button>

                        <button
                          onClick={openReturnModal}
                          disabled={actionLoading || order.confirmed}
                          className={`py-3 rounded-md transition-colors ${
                            order.confirmed
                              ? "bg-gray-600 text-gray-400 cursor-not-allowed"
                              : "bg-orange-500/80 text-white hover:bg-orange-600 disabled:opacity-50 disabled:cursor-not-allowed"
                          }`}
                        >
                          {order.confirmed ? "반품 불가" : actionLoading ? "처리중..." : "반품 요청"}
                        </button>
                      </div>
                    ) : (
                      <button
                        onClick={handleConfirmPurchase}
                        disabled={actionLoading || order.confirmed}
                        className={`w-full py-2 rounded-md transition-colors text-sm ${
                          order.confirmed
                            ? "bg-gray-600 text-gray-400 cursor-not-allowed"
                            : "bg-green-500/80 text-white hover:bg-green-600 disabled:opacity-50 disabled:cursor-not-allowed"
                        }`}
                      >
                        {order.confirmed ? "구매확정 완료" : actionLoading ? "처리중..." : "구매확정"}
                      </button>
                    )}
                  </>
                )}
                {order.status === "RETURN_REQUESTED" && (
                  <button
                    onClick={handleCancelReturn}
                    disabled={actionLoading}
                    className="w-full py-2 rounded-md bg-red-600/80 text-white hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors text-sm"
                  >
                    {actionLoading ? "처리중..." : "반품 요청 취소"}
                  </button>
                )}
              </div>
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

      {/* 반품 요청 모달 */}
      {showReturnModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="absolute inset-0 bg-black/50" onClick={() => setShowReturnModal(false)} />
          <div className="relative bg-white w-[480px] max-w-[90vw] rounded-lg p-6 shadow-lg z-10">
            <h3 className="text-lg font-bold mb-3 text-black">반품 요청</h3>

            {/* 반품 사유 */}
            <div className="mb-4">
              <p className="text-sm font-semibold mb-2 text-black">반품 사유 선택</p>
              <label className="flex items-center gap-2 mb-2 text-sm p-3 border rounded hover:bg-gray-50 cursor-pointer">
                <input
                  type="radio"
                  name="returnReason"
                  value="DEFECT"
                  checked={returnReason === "DEFECT"}
                  onChange={() => setReturnReason("DEFECT")}
                />
                <div className="text-black">
                  <div className="font-medium">제품 하자</div>
                  <div className="text-xs text-gray-600">전액 환불 (배송비 무료)</div>
                </div>
              </label>
              <label className="flex items-center gap-2 text-sm p-3 border rounded hover:bg-gray-50 cursor-pointer">
                <input
                  type="radio"
                  name="returnReason"
                  value="CUSTOM"
                  checked={returnReason === "CUSTOM"}
                  onChange={() => setReturnReason("CUSTOM")}
                />
                <div className="text-black">
                  <div className="font-medium">직접 입력</div>
                  <div className="text-xs text-gray-600">반품 사유를 직접 입력</div>
                </div>
              </label>
              {returnReason === "CUSTOM" && (
                <input
                  type="text"
                  placeholder="반품 사유를 입력해주세요"
                  value={customReturnReason}
                  onChange={(e) => setCustomReturnReason(e.target.value)}
                  className="w-full mt-2 px-3 py-2 border rounded text-black placeholder-gray-400 focus:outline-none focus:border-orange-600"
                />
              )}
            </div>

            {/* 반품 방법 */}
            <div className="mb-4">
              <p className="text-sm font-semibold mb-2 text-black">반품 방법</p>
              <div className="text-sm p-3 border rounded bg-blue-50 text-blue-900">
                <span className="font-medium">회수 요청만 가능합니다</span>
                <p className="text-xs text-blue-800 mt-1">(판매자가 수거해갑니다)</p>
              </div>
            </div>

            {/* 모달 버튼 */}
            <div className="flex justify-end gap-2 mt-4">
              <button
                onClick={() => setShowReturnModal(false)}
                className="px-4 py-2 text-sm rounded border border-gray-300 hover:bg-gray-50 text-black"
              >
                취소
              </button>
              <button
                onClick={submitReturnRequest}
                disabled={!returnReason || !returnMethod || (returnReason === "CUSTOM" && !customReturnReason.trim()) || actionLoading}
                className="px-4 py-2 text-sm rounded bg-orange-600 text-white hover:bg-orange-700 disabled:opacity-50"
              >
                {actionLoading ? "요청 중..." : "반품 요청"}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
