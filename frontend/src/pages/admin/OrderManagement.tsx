import { useEffect, useMemo, useState } from "react";
import { API_BASE_URL } from "../../config/apiBase";

const SERVER = API_BASE_URL;

type Order = {
  id: number;
  orderId: string;
  status: string;
  receiverName?: string;
  address?: string;
  phone?: string;
  totalPrice: number;
  originalPrice?: number;
  couponDiscount?: number;
  pointsDiscount?: number;
  totalDiscount?: number;
  finalPrice?: number;
  createdAt?: string;
  orderDate?: string;
  trackingNumber?: string | null;
  returnTrackingNumber?: string | null;
  returnReason?: string | null;
  returnMethod?: string | null;
  returnShippingFee?: number | null;
  confirmed?: boolean;
  confirmedAt?: string | null;
  deliveredAt?: string | null;
  orderItems?: Array<{
    id: number;
    productId: number;
    productName: string;
    quantity: number;
    orderPrice: number;
    subTotal: number;
    selectedSize?: string;
    selectedColor?: string;
  }>;
  itemsJson?: string;
};

interface OrderManagementProps {
  isOpen: boolean;
  onToggle: () => void;
}

export default function OrderManagement({ isOpen, onToggle }: OrderManagementProps) {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<number | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [sortMode, setSortMode] = useState<"asc" | "desc">("asc");
  const [tabMode, setTabMode] = useState<"ongoing" | "completed">("ongoing");

  const getOrderTime = (o: Order) => {
    const t = new Date((o.orderDate ?? o.createdAt) as string).getTime();
    return Number.isFinite(t) ? t : 0;
  };

  const isCompletedOrder = (status: string): boolean => {
    const key = (status as any)?.toUpperCase?.() ?? status;
    return key === "DELIVERED" || key === "RETURNED" || key === "CANCELLED" || key === "PAYMENT_EXPIRED";
  };

  const displayedOrders = useMemo(() => {
    let arr = [...orders];
    
    // 탭 필터링 (진행중/완료)
    if (tabMode === "ongoing") {
      arr = arr.filter(o => !isCompletedOrder(o.status));
    } else {
      arr = arr.filter(o => isCompletedOrder(o.status));
    }
    
    arr.sort((a, b) => (sortMode === "asc" ? getOrderTime(a) - getOrderTime(b) : getOrderTime(b) - getOrderTime(a)));
    return arr;
  }, [orders, sortMode, tabMode]);

  useEffect(() => {
    fetchOrders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter]);

  const fetchOrders = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem("access_token");
      const query = statusFilter !== "ALL" ? `?status=${statusFilter}` : "";
      const res = await fetch(`${SERVER}/api/admin/orders${query}`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (res.ok) {
        const data = await res.json();
        console.log("주문 데이터:", data);
        
        // 모든 productId 수집 (중복 제거)
        const productIds = new Set<number>();
        data.forEach((order: Order) => {
          order.orderItems?.forEach(item => {
            productIds.add(item.productId);
          });
        });

        // 각 productId로 상품 정보 조회
        const productMap = new Map<number, any>();
        await Promise.all(
          Array.from(productIds).map(async (productId) => {
            try {
              const productRes = await fetch(`${SERVER}/api/admin/products/${productId}`, {
                headers: token ? { Authorization: `Bearer ${token}` } : {},
              });
              if (productRes.ok) {
                const product = await productRes.json();
                productMap.set(productId, product);
              }
            } catch (e) {
              console.error(`상품 ${productId} 조회 실패:`, e);
            }
          })
        );

        // orderItems에 색상 정보 추가
        const ordersWithColors = data.map((order: Order) => ({
          ...order,
          orderItems: order.orderItems?.map(item => ({
            ...item,
            selectedColor: productMap.get(item.productId)?.color || null,
          })),
        }));

        console.log("색상 정보 포함된 주문:", ordersWithColors);
        setOrders(ordersWithColors);
      }
    } catch (e) {
      console.error("관리자 주문 조회 실패:", e);
    } finally {
      setLoading(false);
    }
  };

  const getStatusText = (status: string) => {
    const statusMap: { [key: string]: string } = {
      PAYMENT_PENDING: "가상계좌입금대기",
      PAYMENT_EXPIRED: "가상계좌입금만료",
      ORDERED: "상품 준비중",
      PREPARING_SHIPMENT: "배송준비중",
      SHIPPED: "배송중",
      RETURN_SHIPPING: "반품 배송중",
      DELIVERED: "배송 완료",
      CANCELLED: "주문 취소",
      RETURN_REQUESTED: "반품 요청",
      RETURNED: "반품 완료",
    };
    const key = (status as any)?.toUpperCase?.() ?? status;
    return statusMap[key] || status;
  };

  const getStatusColor = (status: string) => {
    const colorMap: { [key: string]: string } = {
      PAYMENT_PENDING: "text-amber-600",
      PAYMENT_EXPIRED: "text-gray-700",
      ORDERED: "text-yellow-700",
      PREPARING_SHIPMENT: "text-amber-600",
      SHIPPED: "text-indigo-700",
      RETURN_SHIPPING: "text-orange-700",
      DELIVERED: "text-green-700",
      CANCELLED: "text-red-700",
      RETURN_REQUESTED: "text-orange-700",
      RETURNED: "text-gray-700",
    };
    const key = (status as any)?.toUpperCase?.() ?? status;
    return colorMap[key] || "text-gray-600";
  };

  const prepareForShipment = async (id: number) => {
    if (!confirm("이 주문을 배송준비중으로 변경하시겠습니까?")) return;
    setActionLoading(id);
    try {
      const token = localStorage.getItem("access_token");
      const res = await fetch(`${SERVER}/api/admin/orders/${id}/prepare-shipment`, {
        method: "POST",
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (res.ok) {
        await fetchOrders();
      } else {
        const data = await res.json().catch(() => ({}));
        alert(data.error || "배송준비 상태 변경에 실패했습니다.");
      }
    } catch (e) {
      console.error("배송준비 상태 변경 실패:", e);
      alert("오류가 발생했습니다.");
    } finally {
      setActionLoading(null);
    }
  };

  const shipOrder = async (id: number) => {
    const input = prompt("송장번호만 입력. 대한통운 고정\n(ex:123-456-7890)");
    if (input === null) return;
    const trackingNumber = input.trim();
    if (!trackingNumber) {
      alert("송장번호를 입력해주세요.");
      return;
    }
    setActionLoading(id);
    try {
      const token = localStorage.getItem("access_token");
      const setTn = await fetch(
        `${SERVER}/api/admin/orders/${id}/tracking?trackingNumber=${encodeURIComponent(trackingNumber)}`,
        {
          method: "POST",
          headers: token ? { Authorization: `Bearer ${token}` } : {},
        }
      );
      if (!setTn.ok) {
        const data = await setTn.json().catch(() => ({}));
        alert(data.error || "송장번호 설정에 실패했습니다.");
        return;
      }
      const shipRes = await fetch(`${SERVER}/api/admin/orders/${id}/ship`, {
        method: "POST",
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (shipRes.ok) {
        await fetchOrders();
      } else {
        const data = await shipRes.json().catch(() => ({}));
        alert(data.error || "배송 시작에 실패했습니다.");
      }
    } catch (e) {
      console.error("배송 시작 실패:", e);
      alert("오류가 발생했습니다.");
    } finally {
      setActionLoading(null);
    }
  };

  const deliverOrder = async (id: number) => {
    if (!confirm("이 주문을 배송 완료로 처리하시겠습니까?")) return;
    setActionLoading(id);
    try {
      const token = localStorage.getItem("access_token");
      const res = await fetch(`${SERVER}/api/admin/orders/${id}/deliver`, {
        method: "POST",
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (res.ok) {
        await fetchOrders();
      } else {
        const data = await res.json().catch(() => ({}));
        alert(data.error || "배송 완료 처리에 실패했습니다.");
      }
    } catch (e) {
      console.error("배송 완료 실패:", e);
      alert("오류가 발생했습니다.");
    } finally {
      setActionLoading(null);
    }
  };

  const approveReturn = async (id: number) => {
    const input = prompt("수거 송장번호를 입력하세요 (반품 승인 시 필요)");
    if (input === null) return;
    const returnTrackingNumber = input.trim();
    if (!returnTrackingNumber) {
      alert("수거 송장번호를 입력해주세요.");
      return;
    }
    setActionLoading(id);
    try {
      const token = localStorage.getItem("access_token");
      const res = await fetch(
        `${SERVER}/api/admin/orders/${id}/approve-return?returnTrackingNumber=${encodeURIComponent(returnTrackingNumber)}`,
        {
          method: "POST",
          headers: token ? { Authorization: `Bearer ${token}` } : {},
        }
      );
      if (res.ok) {
        await fetchOrders();
      } else {
        const data = await res.json().catch(() => ({}));
        alert(data.error || "반품 승인에 실패했습니다.");
      }
    } catch (e) {
      console.error("반품 승인 실패:", e);
      alert("오류가 발생했습니다.");
    } finally {
      setActionLoading(null);
    }
  };

  const cancelReturn = async (id: number) => {
    if (!confirm("이 반품 요청을 취소하시겠습니까?")) return;
    setActionLoading(id);
    try {
      const token = localStorage.getItem("access_token");
      const res = await fetch(`${SERVER}/api/admin/orders/${id}/cancel-return`, {
        method: "POST",
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (res.ok) {
        await fetchOrders();
      } else {
        const data = await res.json().catch(() => ({}));
        alert(data.error || "반품 요청 취소에 실패했습니다.");
      }
    } catch (e) {
      console.error("반품 요청 취소 실패:", e);
      alert("오류가 발생했습니다.");
    } finally {
      setActionLoading(null);
    }
  };

  const updateTrackingNumber = async (id: number) => {
    const input = prompt("수정할 송장번호를 입력하세요 (예: 123-456-7890)");
    if (input === null) return;
    const trackingNumber = input.trim();
    if (!trackingNumber) {
      alert("송장번호를 입력해주세요.");
      return;
    }
    setActionLoading(id);
    try {
      const token = localStorage.getItem("access_token");
      const res = await fetch(
        `${SERVER}/api/admin/orders/${id}/tracking?trackingNumber=${encodeURIComponent(trackingNumber)}`,
        {
          method: "POST",
          headers: token ? { Authorization: `Bearer ${token}` } : {},
        }
      );
      if (res.ok) {
        alert("송장번호가 수정되었습니다.");
        await fetchOrders();
      } else {
        const data = await res.json().catch(() => ({}));
        alert(data.error || "송장번호 수정에 실패했습니다.");
      }
    } catch (e) {
      console.error("송장번호 수정 실패:", e);
      alert("오류가 발생했습니다.");
    } finally {
      setActionLoading(null);
    }
  };

  const completeReturnPartial = async (id: number) => {
    const feeInput = prompt(
      "차감할 반품 배송비를 입력하세요 (숫자, 원)",
      "3000"
    );
    if (feeInput === null) return;
    const parsed = Number(feeInput.replace(/[^0-9]/g, ""));
    if (Number.isNaN(parsed) || parsed < 0) {
      alert("유효한 금액을 입력해주세요.");
      return;
    }

    if (!confirm(`배송비 ${parsed.toLocaleString()}원을 차감하고 반품을 완료 처리(환불)하시겠습니까?`)) return;

    setActionLoading(id);
    try {
      const token = localStorage.getItem("access_token");
      const res = await fetch(`${SERVER}/api/admin/orders/${id}/complete-return`, {
        method: "POST",
        headers: token
          ? { Authorization: `Bearer ${token}`, "Content-Type": "application/json" }
          : { "Content-Type": "application/json" },
        body: JSON.stringify({ returnReason: "단순변심", returnShippingFee: parsed }),
      });
      if (res.ok) {
        await fetchOrders();
      } else {
        const data = await res.json().catch(() => ({}));
        alert(data.error || "반품 완료 처리에 실패했습니다.");
      }
    } catch (e) {
      console.error("반품 완료 처리 실패:", e);
      alert("오류가 발생했습니다.");
    } finally {
      setActionLoading(null);
    }
  };

  const completeReturnFull = async (id: number) => {
    if (!confirm("전액 환불로 반품을 완료 처리하시겠습니까?")) return;

    setActionLoading(id);
    try {
      const token = localStorage.getItem("access_token");
      const res = await fetch(`${SERVER}/api/admin/orders/${id}/complete-return`, {
        method: "POST",
        headers: token
          ? { Authorization: `Bearer ${token}`, "Content-Type": "application/json" }
          : { "Content-Type": "application/json" },
        body: JSON.stringify({ returnReason: "제품하자", returnShippingFee: 0 }),
      });
      if (res.ok) {
        await fetchOrders();
      } else {
        const data = await res.json().catch(() => ({}));
        alert(data.error || "반품 완료 처리에 실패했습니다.");
      }
    } catch (e) {
      console.error("반품 완료 처리 실패:", e);
      alert("오류가 발생했습니다.");
    } finally {
      setActionLoading(null);
    }
  };

  return (
    <div className="border p-4 rounded mb-8 font-sans">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <h2 className="text-xl font-semibold">주문 관리</h2>
          {!loading && (
            <span className="text-sm text-orange-800">({orders.length})</span>
          )}
        </div>
        <button
          onClick={onToggle}
          className="px-3 py-1 border rounded text-sm bg-blue-300 hover:bg-blue-200"
        >
          {isOpen ? "접기" : "펼치기"}
        </button>
      </div>

      {isOpen && (
        <>
          <div className="flex items-center justify-between mt-4 mb-4">
            <div className="flex gap-2">
              <button
                onClick={() => setTabMode("ongoing")}
                className={`px-4 py-2 rounded font-medium transition-colors ${
                  tabMode === "ongoing"
                    ? "bg-blue-500 text-white"
                    : "bg-gray-300 text-gray-700 hover:bg-gray-400"
                }`}
              >
                진행중인 거래 ({orders.filter(o => !isCompletedOrder(o.status)).length})
              </button>
              <button
                onClick={() => setTabMode("completed")}
                className={`px-4 py-2 rounded font-medium transition-colors ${
                  tabMode === "completed"
                    ? "bg-blue-500 text-white"
                    : "bg-gray-300 text-gray-700 hover:bg-gray-400"
                }`}
              >
                완료된 거래 ({orders.filter(o => isCompletedOrder(o.status)).length})
              </button>
            </div>
            <button
              onClick={onToggle}
              className="px-3 py-1 border rounded text-sm bg-blue-300 hover:bg-blue-200"
            >
              {isOpen ? "접기" : "펼치기"}
            </button>
          </div>

          <div className="flex items-center justify-end gap-2 mt-4">
            <label className="text-sm text-gray-600">상태 필터</label>
            <select
              className="border rounded px-2 py-1 text-sm bg-blue-300 hover:bg-blue-200"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="ALL">전체</option>
              <option value="PAYMENT_PENDING">입금대기</option>
              <option value="PAYMENT_EXPIRED">입금만료</option>
              <option value="ORDERED">상품 준비중</option>
              <option value="PREPARING_SHIPMENT">배송준비중</option>
              <option value="SHIPPED">배송중</option>
              <option value="RETURN_SHIPPING">반품 배송중</option>
              <option value="DELIVERED">배송 완료</option>
              <option value="CANCELLED">주문 취소</option>
              <option value="RETURN_REQUESTED">반품 요청</option>
              <option value="RETURNED">반품 완료</option>
            </select>
            <button
              onClick={() => setSortMode((m) => (m === "asc" ? "desc" : "asc"))}
              className="px-3 py-1 border rounded text-sm bg-blue-300 hover:bg-blue-200"
            >
              {sortMode === "asc" ? "최신순" : "시간순"}
            </button>
            <button
              onClick={fetchOrders}
              className="px-3 py-1 border rounded text-sm bg-blue-300 hover:bg-blue-200"
            >
              새로고침
            </button>
          </div>

          <div className="mt-4">
            {loading ? (
              <p className="text-black">로딩 중...</p>
            ) : orders.length === 0 ? (
              <p className="text-black">주문이 없습니다.</p>
            ) : (
              <div className="space-y-3">
                {displayedOrders.map((order) => {
                  const status = (order.status as any)?.toUpperCase?.() ?? order.status;
                  return (
                    <div key={order.id} className="border rounded p-4">
                      <div className="flex items-start justify-between mb-2">
                        <div>
                          <p className="text-sm text-black mb-1">
                            주문번호: <span className="font-mono">{order.orderId}</span>
                          </p>
                          <p className="text-xs text-black">
                            주문일: {new Date(order.orderDate ?? order.createdAt ?? "").toLocaleString("ko-KR")}
                          </p>
                        </div>
                        <span className={`text-sm font-bold ${getStatusColor(order.status)}`}>
                          {getStatusText(order.status)}
                        </span>
                      </div>

                      {/* 주문자/배송지 정보 */}
                      {(order.receiverName || order.phone || order.address) && (
                        <div className="mb-3 p-3 bg-blue-50 rounded border border-blue-200">
                          <p className="text-xs font-semibold text-blue-800 mb-1">받는 사람 정보</p>
                          {order.receiverName && (
                            <p className="text-sm text-gray-800">이름: {order.receiverName}</p>
                          )}
                          {order.phone && (
                            <p className="text-sm text-gray-800">전화번호: {order.phone}</p>
                          )}
                          {order.address && (
                            <p className="text-sm text-gray-800 mt-1">주소: {order.address}</p>
                          )}
                        </div>
                      )}

                      {/* 주문 상품 정보 */}
                      {order.orderItems && order.orderItems.length > 0 && (
                        <div className="mb-3 p-3 bg-gray-50 rounded border border-gray-200">
                          <p className="text-xs font-semibold text-gray-800 mb-2">주문 상품</p>
                          {order.orderItems.map((item, idx) => (
                            <div key={idx} className="text-sm text-gray-800 mb-1">
                              <span className="font-medium">{item.productName}</span>
                              <span className="text-gray-600"> × {item.quantity}개</span>
                              {item.selectedColor && (
                                <span className="text-gray-600"> / 색상: {item.selectedColor}</span>
                              )}
                              {item.selectedSize && (
                                <span className="text-gray-600"> / 사이즈: {item.selectedSize}</span>
                              )}
                              <span className="text-gray-600 ml-2">({item.subTotal.toLocaleString()}원)</span>
                            </div>
                          ))}
                        </div>
                      )}

                      <div className="flex items-center justify-between pt-3 border-t">
                        <span className="text-base font-semibold">결제 금액</span>
                        <span className="text-lg font-bold">
                          {order.totalPrice.toLocaleString()}원
                        </span>
                      </div>

                      {order.trackingNumber && (
                        <div className="mt-2 text-sm text-gray-700">
                          송장번호: <span className="font-mono">{order.trackingNumber}</span>
                        </div>
                      )}

                      {(status === "RETURN_REQUESTED" || status === "RETURN_SHIPPING" || status === "RETURNED") && (
                        <div className="mt-2 p-2 bg-orange-50 rounded border border-orange-200">
                          {order.returnReason && (
                            <div className="text-sm text-orange-800 mb-1">
                              <span className="font-semibold">반품 사유:</span>{" "}
                              {order.returnReason === "DEFECT" ? "제품 하자" : order.returnReason === "CHANGE_OF_MIND" ? "단순 변심" : order.returnReason}
                            </div>
                          )}
                          {order.returnMethod && (
                            <div className="text-sm text-orange-800 mb-1">
                              <span className="font-semibold">반품 방법:</span>{" "}
                              {order.returnMethod === "SELF" ? "직접 반품" : order.returnMethod === "PICKUP" ? "회수 요청" : order.returnMethod}
                            </div>
                          )}
                
                
                          
                        </div>
                      )}

                      {order.returnTrackingNumber && (
                        <div className="mt-2 text-sm text-gray-700">
                          반품 송장번호: <span className="font-mono">{order.returnTrackingNumber}</span>
                        </div>
                      )}

                      <div className="mt-3 flex gap-2 justify-end">
                        {status === "ORDERED" && (
                          <button
                            onClick={() => prepareForShipment(order.id)}
                            disabled={actionLoading === order.id}
                            className="px-3 py-1.5 bg-amber-600 text-white text-sm rounded hover:bg-amber-700 disabled:opacity-50"
                          >
                            {actionLoading === order.id ? "처리중..." : "배송준비중"}
                          </button>
                        )}
                        {status === "PREPARING_SHIPMENT" && (
                          <button
                            onClick={() => shipOrder(order.id)}
                            disabled={actionLoading === order.id}
                            className="px-3 py-1.5 bg-indigo-600 text-white text-sm rounded hover:bg-indigo-700 disabled:opacity-50"
                          >
                            {actionLoading === order.id ? "처리중..." : "배송 시작"}
                          </button>
                        )}
                        {status === "SHIPPED" && (
                          <>
                            <button
                              onClick={() => deliverOrder(order.id)}
                              disabled={actionLoading === order.id}
                              className="px-3 py-1.5 bg-green-600 text-white text-sm rounded hover:bg-green-700 disabled:opacity-50"
                            >
                              {actionLoading === order.id ? "처리중..." : "배송 완료"}
                            </button>
                            <button
                              onClick={() => updateTrackingNumber(order.id)}
                              disabled={actionLoading === order.id}
                              className="px-3 py-1.5 bg-blue-600 text-white text-sm rounded hover:bg-blue-700 disabled:opacity-50"
                            >
                              {actionLoading === order.id ? "처리중..." : "송장번호 수정"}
                            </button>
                          </>
                        )}
                        {status === "RETURN_REQUESTED" && (
                          <>
                            <button
                              onClick={() => approveReturn(order.id)}
                              disabled={actionLoading === order.id}
                              className="px-3 py-1.5 bg-orange-600 text-white text-sm rounded hover:bg-orange-700 disabled:opacity-50"
                            >
                              {actionLoading === order.id ? "처리중..." : "반품 승인(수거 송장)"}
                            </button>
                            <button
                              onClick={() => cancelReturn(order.id)}
                              disabled={actionLoading === order.id}
                              className="px-3 py-1.5 bg-red-600 text-white text-sm rounded hover:bg-red-700 disabled:opacity-50"
                            >
                              {actionLoading === order.id ? "처리중..." : "반품 요청 취소"}
                            </button>
                          </>
                        )}
                        {status === "RETURN_SHIPPING" && (
                          <>
                            <button
                              onClick={() => completeReturnPartial(order.id)}
                              disabled={actionLoading === order.id}
                              className="px-3 py-1.5 bg-orange-600 text-white text-sm rounded hover:bg-orange-700 disabled:opacity-50"
                            >
                              {actionLoading === order.id ? "처리중..." : "일부환불 (배송비 차감)"}
                            </button>
                            <button
                              onClick={() => completeReturnFull(order.id)}
                              disabled={actionLoading === order.id}
                              className="px-3 py-1.5 bg-gray-700 text-white text-sm rounded hover:bg-gray-800 disabled:opacity-50"
                            >
                              {actionLoading === order.id ? "처리중..." : "전액환불"}
                            </button>
                          </>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
