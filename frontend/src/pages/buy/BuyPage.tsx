import { useEffect, useMemo, useRef, useState } from "react";
import api from "../../lib/axios";
import { useLocation, Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/useAuth";
import { API_BASE_URL } from "../../config/apiBase";

const SERVER = API_BASE_URL;

export default function BuyPage() {
  const { user } = useAuth();
  const [name, setName] = useState("");
  const [address, setAddress] = useState("");
  const [addressDetail, setAddressDetail] = useState("");
  const [phone, setPhone] = useState("");
  const [savedAddresses, setSavedAddresses] = useState<string[]>([]);
  const [showAddressSelect, setShowAddressSelect] = useState(false);
  const [isAddressFromSaved, setIsAddressFromSaved] = useState(false);
  // 수량은 이 페이지에서 선택하지 않습니다 (장바구니/바로구매에서 전달)

  const detailRef = useRef<HTMLInputElement | null>(null);
  const popupRef = useRef<Window | null>(null);
  const location = useLocation();
  const navigate = useNavigate();

  type BuyNowItem = { productId: number; productName: string; price: number; quantity: number; size?: string; color?: string; mainImageUrl?: string };
  type CartItem = { id: number; quantity: number; productId: number; productName: string; price: number; mainImageUrl?: string; size?: string; color?: string };
  type StateShape =
    | { mode: 'buy-now'; item: BuyNowItem }
    | { mode: 'cart'; items: CartItem[] }
    | undefined;

  const state = location.state as StateShape;

  // 표준화된 아이템 목록으로 정규화
  const items = useMemo(() => {
    if (!state) return [] as BuyNowItem[];
    if ((state as any).mode === 'buy-now') {
      const i = (state as any).item as BuyNowItem;
      return [{ productId: i.productId, productName: i.productName, price: i.price, quantity: i.quantity, size: i.size, color: i.color, mainImageUrl: i.mainImageUrl }];
    }
    if ((state as any).mode === 'cart') {
      const arr = (state as any).items as CartItem[];
      return arr.map(i => ({ productId: i.productId, productName: i.productName, price: i.price, quantity: i.quantity, size: i.size, color: i.color, mainImageUrl: i.mainImageUrl }));
    }
    return [] as BuyNowItem[];
  }, [state]);

  const totals = useMemo(() => {
    const totalCount = items.reduce((s, i) => s + i.quantity, 0);
    const totalPrice = items.reduce((s, i) => s + i.price * i.quantity, 0);
    return { totalCount, totalPrice };
  }, [items]);

  // 저장된 주소 목록 불러오기
  useEffect(() => {
    if (user?.id) {
      fetchSavedAddresses();
    }
  }, [user]);

  const fetchSavedAddresses = async () => {
    if (!user?.id) return;
    try {
      const token = localStorage.getItem("access_token");
      const response = await fetch(`${SERVER}/api/user/${user.id}/addresses`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (response.ok) {
        const data = await response.json();
        setSavedAddresses(data);
      }
    } catch (error) {
      console.error("주소 조회 실패:", error);
    }
  };

  // ✅ postMessage 수신 (보안 체크 제거 버전)
  useEffect(() => {
    function onMessage(e: MessageEvent) {
      // 원하신 대로 origin 검사를 없앴습니다.
      const payload = e.data as any;
      if (!payload) return;

      // juso-search.html에서 내려주는 표준 필드
      const full =
        payload.fullAddress ||
        payload.roadAddr ||
        payload.roadAddrPart1 ||
        payload.jibunAddr ||
        "";

      if (full) {
        setAddress(full);
        setIsAddressFromSaved(false); // 새로 검색한 주소이므로 저장된 주소 아님
        setTimeout(() => {
          detailRef.current?.focus();
        }, 50);
      }

      // 팝업 닫기
      if (popupRef.current && !popupRef.current.closed) popupRef.current.close();
      popupRef.current = null;
    }
    window.addEventListener("message", onMessage);
    return () => window.removeEventListener("message", onMessage);
  }, []);

  const handleBuy = async (e: React.FormEvent) => {
    e.preventDefault();

    // 로그인 필요 검사
    if (!user) {
      alert('로그인이 필요합니다. 로그인 후 다시 시도해주세요.');
      navigate('/login');
      return;
    }

    // 간단 유효성 (선택)
    if (!name.trim()) return alert("이름을 입력해주세요.");
    if (!address.trim()) return alert("주소를 입력 또는 검색해주세요.");
    if (!phone.trim()) return alert("전화번호를 입력해주세요.");
    if (items.length === 0) return alert("주문할 상품이 없습니다. 장바구니 또는 상품 페이지에서 다시 시도하세요.");

    // 주문 상품 정보를 JSON 문자열로 변환 (사이즈 포함)
    const itemsForServer = items.map(item => ({
      productId: item.productId,
      quantity: item.quantity,
      size: item.size || null  // 사이즈 정보 추가
    }));

    // 서버 엔티티에 맞춘 페이로드
    const checkoutRequest = {
      userId: user?.id,  // ✅ 주문자 ID 추가
      name: name,
      address: addressDetail ? `${address} ${addressDetail}` : address,
      phone: phone,
      itemsJson: JSON.stringify(itemsForServer),  // ✅ JSON 문자열로 전송
    };

    // JWT 토큰을 Authorization 헤더로 포함
    try {
      const res = await api.post(
        `/api/checkout`,
        checkoutRequest,
        { validateStatus: (s) => s >= 200 && s < 300 }
      );

      const data = res.data as any;
      console.log('checkout save response:', data); // 저장 후 받은 데이터 로그
      // 서버는 보통 생성된 checkout의 id를 반환합니다.
      const checkoutId = data.id || data.checkoutId;
      if (!checkoutId) {
        console.warn('no checkout id in response', data);
        return alert('결제 준비에 실패했습니다. 관리자에게 문의하세요.');
      }

      // 요약 정보 저장 (새로고침 대비 sessionStorage + state 전달)
      try {
        const summary = { items, totalCount: totals.totalCount, totalPrice: totals.totalPrice };
        sessionStorage.setItem(`checkout:${checkoutId}:summary`, JSON.stringify(summary));
      } catch {}
      navigate(`/payment?checkoutId=${encodeURIComponent(checkoutId)}`, {
        state: { items, totalCount: totals.totalCount, totalPrice: totals.totalPrice }
      });
    } catch (err: any) {
      console.error(err);
      const status = err?.response?.status ?? 0;
      if (status === 401) {
        alert('로그인이 필요합니다. 로그인 후 다시 시도해주세요.');
        window.location.href = '/login';
      } else {
        alert('요청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도하세요.');
      }
    }
  };

  // ✅ 프론트 전용 팝업 열기 (동일 출처 HTML)
  const openJusoPopup = () => {
    setIsAddressFromSaved(false); // 새로 검색하려는 것이므로 저장된 주소 선택 해제
    const url = `${window.location.origin}/juso-search.html`;
    popupRef.current = window.open(
      url,
      "jusoSearch",
      "width=600,height=640,menubar=no,toolbar=no,location=no,status=no,resizable=yes,scrollbars=yes"
    );
    if (popupRef.current) {
      popupRef.current.focus();
    }
  };

  return (
    <div className="min-h-screen bg-gray-400" style={{ fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', 'Arial', sans-serif" }}>
      <div className="max-w-lg mx-auto px-4 py-24">
        <h2 className="text-2xl font-bold mb-6">구매하기</h2>
      <form onSubmit={handleBuy} className="space-y-4">
        <div>
          <label className="block mb-1 font-medium">이름</label>
          <input
            type="text"
            value={name}
            onChange={(e)=>setName(e.target.value)}
            className="w-full border border-gray-500 rounded px-3 py-2 bg-gray-300"
            required
          />
        </div>

        <div>
          <label className="block mb-1 font-medium">주소</label>
          
          {/* 저장된 주소가 있으면 선택 옵션 표시 */}
          {savedAddresses.length > 0 && (
            <div className="mb-2">
              <button
                type="button"
                onClick={() => setShowAddressSelect(!showAddressSelect)}
                className="text-sm text-blue-700 hover:underline font-medium"
              >
                {showAddressSelect ? "새 주소 입력하기" : "저장된 주소에서 선택하기"}
              </button>
            </div>
          )}

          {showAddressSelect && savedAddresses.length > 0 ? (
            // 저장된 주소 선택 모드
            <div className="space-y-2">
              {savedAddresses.map((addr, index) => (
                <div
                  key={index}
                  onClick={() => {
                    setAddress(addr);
                    setAddressDetail("");
                    setShowAddressSelect(false);
                    setIsAddressFromSaved(true);
                  }}
                  className="p-3 border border-gray-500 rounded cursor-pointer hover:bg-gray-400 transition-colors bg-gray-300"
                >
                  <span className="text-sm">{addr}</span>
                </div>
              ))}
            </div>
          ) : (
            // 새 주소 입력 모드
            <>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={address}
                  readOnly
                  onClick={openJusoPopup}
                  className="flex-1 border border-gray-500 rounded px-3 py-2 bg-gray-300 cursor-pointer"
                  placeholder="주소검색 버튼을 눌러 선택하세요"
                  title="주소는 검색으로만 입력됩니다"
                  required
                />
                <button
                  type="button"
                  onClick={openJusoPopup}
                  className="px-4 py-2 bg-gray-700 text-white rounded hover:bg-blue-700"
                >
                  주소검색
                </button>
              </div>
              {/* 저장된 주소를 선택하지 않은 경우에만 상세주소 입력 필드 표시 */}
              {!isAddressFromSaved && (
                <input
                  ref={detailRef}
                  type="text"
                  value={addressDetail}
                  onChange={(e)=>setAddressDetail(e.target.value)}
                  placeholder="상세주소 (동/층/호)"
                  className="mt-2 w-full border border-gray-500 rounded px-3 py-2 bg-gray-300"
                />
              )}
            </>
          )}
        </div>

        <div>
          <label className="block mb-1 font-medium">전화번호</label>
          <input
            type="text"
            value={phone}
            onChange={(e)=>setPhone(e.target.value)}
            className="w-full border border-gray-500 rounded px-3 py-2 bg-gray-300"
            placeholder="- 없이 숫자만 입력해주세요 (예: 01012345678)"
            required
          />
        </div>

        {/* 주문 요약 */}
        <div className="mt-6 border border-gray-500 rounded-lg p-4 bg-gray-300">
          <div className="font-semibold mb-2">주문 내역</div>
          {items.length === 0 ? (
            <div className="text-gray-700">
              주문할 상품이 없습니다. {" "}
              <Link to="/cart" className="text-gray-900 underline font-medium">장바구니</Link> 또는 {" "}
              <Link to="/cap" className="text-gray-900 underline font-medium">상품 목록</Link>에서 선택하세요.
            </div>
          ) : (
            <>
              <ul className="space-y-3">
                {items.map((it, idx) => (
                  <li key={idx} className="flex gap-3 border rounded p-2 bg-white">
                    {/* 제품 사진 */}
                    {it.mainImageUrl && (
                      <div className="flex-shrink-0 w-16 h-16">
                        <img 
                          src={it.mainImageUrl} 
                          alt={it.productName}
                          className="w-full h-full object-cover rounded"
                        />
                      </div>
                    )}
                    {/* 제품 정보 */}
                    <div className="flex-1 text-sm">
                      <div className="font-semibold">{it.productName}</div>
                      {it.color && (
                        <div className="text-gray-600 text-xs">색상: {it.color}</div>
                      )}
                      {it.size && (
                        <div className="text-gray-600 text-xs">사이즈: {it.size}</div>
                      )}
                      <div className="text-gray-600 text-xs">수량: {it.quantity}개</div>
                      <div className="mt-1 font-semibold text-blue-600">{(it.price * it.quantity).toLocaleString()}원</div>
                    </div>
                  </li>
                ))}
              </ul>
              <div className="mt-3 pt-3 border-t flex justify-between font-semibold">
                <span>총 수량</span>
                <span>{totals.totalCount}개</span>
              </div>
              <div className="mt-1 flex justify-between font-bold text-lg">
                <span>결제 예정 금액</span>
                <span>{totals.totalPrice.toLocaleString()}원</span>
              </div>
            </>
          )}
        </div>

        <button
          type="submit"
          className="w-full py-2 bg-gray-600 text-white rounded font-semibold hover:bg-blue-700"
        >
          다음
        </button>
      </form>
      </div>
    </div>
  );
}
