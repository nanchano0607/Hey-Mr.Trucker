import { useEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import api from '../../lib/axios';
import { getAccessToken } from '../../lib/token';
import { useAuth } from '../../auth/useAuth';
import { API_BASE_URL } from '../../config/apiBase';

const SERVER = API_BASE_URL;

declare global {
  interface Window {
    TossPayments?: any;
  }
}

type Checkout = {
  id?: number;
  orderId: string;
  name: string;
  address: string;
  phone: string;
  quantity: number;
  amount?: number; // 서버가 계산/확정해서 내려주는 최종 결제금액
};

type Summary = {
  items: Array<{ productName: string; price: number; quantity: number; size?: string }>;
  totalCount: number;
  totalPrice: number;
};

type Coupon = {
  id: number;
  name: string;
  type: 'PERCENTAGE' | 'AMOUNT';
  discountValue: number;
  minOrderAmount?: number;
  maxDiscountAmount?: number;
  validUntil: string;
};

type UserCouponResponse = {
  id: number;
  coupon: Coupon;
  status: string;
  obtainedAt: string;
  usedAt?: string;
  usedOrderId?: string;
  discountAmount?: number;
  isAvailable: boolean;
};



export default function PaymentPage() {
  const params = useMemo(() => new URLSearchParams(window.location.search), []);
  const checkoutId = params.get('checkoutId') || '';
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string>('');
  const [checkout, setCheckout] = useState<Checkout | null>(null);
  const [summary, setSummary] = useState<Summary | null>(null);

  // 할인 관련 상태
  const [userPoints, setUserPoints] = useState<number>(0);
  const [availableCoupons, setAvailableCoupons] = useState<UserCouponResponse[]>([]);
  const [selectedCoupon, setSelectedCoupon] = useState<UserCouponResponse | null>(null);
  const [usePoints, setUsePoints] = useState<number>(0);
  const [discountsLoading, setDiscountsLoading] = useState<boolean>(false);
  
  // 사용자 이메일
  const [customerEmail, setCustomerEmail] = useState<string>('');

  // TossPayments
  const [tossReady, setTossReady] = useState(false);
  const tossWidgetsRef = useRef<any>(null);
  const paymentMethodRef = useRef<HTMLDivElement>(null);
  const agreementRef = useRef<HTMLDivElement>(null);

  // 금액 보정: 음수/NaN 방지 + 원단위 내림
  const clampKRW = (v: number) => Math.max(0, Math.floor(v || 0));

  const SHIPPING_FEE = 3500;
  const FREE_SHIPPING_THRESHOLD = 70000;

  // 쿠폰 할인 금액 계산
  const calculateCouponDiscount = (userCoupon: UserCouponResponse | null, orderAmount: number): number => {
    console.log('쿠폰 할인 계산 시작:', { userCoupon, orderAmount });
    
    if (!userCoupon?.coupon || orderAmount <= 0) {
      return 0;
    }
    
    const coupon = userCoupon.coupon;
    
    // 최소 주문 금액 확인
    if (coupon.minOrderAmount && orderAmount < coupon.minOrderAmount) {
      console.log('최소 주문 금액 미달:', { minOrderAmount: coupon.minOrderAmount, orderAmount });
      return 0;
    }
    
    let discount = 0;
    
    // ✅ 백엔드 응답 구조에 맞게 수정
    if (coupon.type === 'PERCENTAGE') {  // discountType → type으로 변경
      discount = Math.floor(orderAmount * (coupon.discountValue / 100));
      // 최대 할인 금액 제한
      if (coupon.maxDiscountAmount && discount > coupon.maxDiscountAmount) {
        discount = coupon.maxDiscountAmount;
      }
      console.log('퍼센트 할인 계산:', { discountValue: coupon.discountValue, discount, maxDiscountAmount: coupon.maxDiscountAmount });
    } else if (coupon.type === 'AMOUNT') {  // FIXED → AMOUNT로 변경
      discount = coupon.discountValue;
      console.log('고정 할인 계산:', { discountValue: coupon.discountValue, discount });
    }
    
    const finalDiscount = Math.min(discount, orderAmount);
    console.log('최종 쿠폰 할인 금액:', finalDiscount);
    return finalDiscount;
  };

  const isCouponSelectable = (userCoupon: UserCouponResponse | null, orderAmount: number) => {
    if (!userCoupon?.coupon) return false;
    if (userCoupon.isAvailable === false) return false;

    const untilRaw = userCoupon.coupon.validUntil;
    const until = untilRaw ? Date.parse(untilRaw) : NaN;
    if (!Number.isNaN(until) && until < Date.now()) return false;

    const min = userCoupon.coupon.minOrderAmount;
    if (typeof min === 'number' && orderAmount < min) return false;
    return true;
  };

  const productAmount = useMemo(() => summary?.totalPrice || 0, [summary]);
  const couponDiscountAmount = useMemo(
    () => calculateCouponDiscount(selectedCoupon, productAmount),
    [selectedCoupon, productAmount]
  );
  const pointsDiscountAmount = useMemo(
    () => Math.min(usePoints, Math.max(0, productAmount - couponDiscountAmount)),
    [usePoints, productAmount, couponDiscountAmount]
  );

  // 상품 금액(배송비 제외)에서만 할인/포인트 적용
  const discountedProductAmount = useMemo(() => {
    const v = Math.max(0, productAmount - couponDiscountAmount - pointsDiscountAmount);

    console.log('할인 계산:', {
      productAmount,
      selectedCoupon: selectedCoupon?.coupon,
      couponDiscountAmount,
      usePoints,
      pointsDiscountAmount,
      discountedProductAmount: v
    });

    return v;
  }, [productAmount, couponDiscountAmount, pointsDiscountAmount, selectedCoupon, usePoints]);

  // 무료배송 기준: "상품 금액"(배송비 제외)이 7만원 이상일 때만 무료
  const shippingFee = useMemo(
    () => (productAmount >= FREE_SHIPPING_THRESHOLD ? 0 : SHIPPING_FEE),
    [productAmount]
  );

  // 토스 결제 요청 금액(배송비 포함)
  const payableAmount = useMemo(
    () => clampKRW(discountedProductAmount + shippingFee),
    [discountedProductAmount, shippingFee]
  );

  // 선택된 쿠폰이 조건을 만족하지 않게 되면 자동 해제
  useEffect(() => {
    const orderAmount = summary?.totalPrice || 0;
    if (!selectedCoupon) return;
    if (!isCouponSelectable(selectedCoupon, orderAmount)) {
      setSelectedCoupon(null);
    }
  }, [selectedCoupon, summary]);

  // 포인트와 쿠폰 정보 조회
  const fetchUserDiscounts = async () => {
    if (!user?.id) {
      return;
    }
    
    setDiscountsLoading(true);
    try {
      const token = getAccessToken();
      
      // 사용자 정보 조회 (이메일 포함)
      const userResponse = await fetch(`${SERVER}/api/user/${user.id}`, {
        headers: { 
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (userResponse.ok) {
        const userData = await userResponse.json();
        setCustomerEmail(userData.email || '');
      }
      
      // 포인트 조회
      const pointsResponse = await fetch(`${SERVER}/api/points/user/${user.id}`, {
        headers: { 
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (!pointsResponse.ok) {
        throw new Error(`포인트 조회 실패: ${pointsResponse.status}`);
      }
      
      const pointsData = await pointsResponse.json();
      setUserPoints(pointsData.points || 0);

      // 쿠폰 조회 (사용 가능한 쿠폰만)
      console.log('쿠폰 조회 요청:', `${SERVER}/api/user-coupons/user/${user.id}/available`);
      try {
        const couponsResponse = await fetch(`${SERVER}/api/user-coupons/user/${user.id}/available`, {
          headers: { 
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });
        
        if (couponsResponse.ok) {
          const couponsData = await couponsResponse.json();
          console.log('쿠폰 조회 응답:', couponsData);
          console.log('첫 번째 쿠폰 상세:', couponsData[0]);
          setAvailableCoupons(Array.isArray(couponsData) ? couponsData : []);
        } else {
          console.log('쿠폰 조회 실패 (404 등은 정상):', couponsResponse.status);
          setAvailableCoupons([]);
        }
      } catch (couponError) {
        console.log('쿠폰 조회 오류 (선택적 기능):', couponError);
        setAvailableCoupons([]);
      }
      
    } catch (error) {
      console.error('할인 정보 조회 실패:', error);
      if (error instanceof Error) {
        console.error('오류 메시지:', error.message);
      }
      // API 오류 시 안전한 기본값 설정
      setUserPoints(0);
      setAvailableCoupons([]);
    } finally {
      setDiscountsLoading(false);
    }
  };

  // 체크아웃 로드
  useEffect(() => {
    let ignore = false;
    async function load() {
      if (!checkoutId) {
        setError('checkoutId가 없습니다.');
        setLoading(false);
        return;
      }
      setLoading(true);
      setError('');
      try {
        const res = await api.get(`/api/checkout/${encodeURIComponent(checkoutId)}`, {
          validateStatus: (s) => (s >= 200 && s < 300) || s === 404,
        });

        if (ignore) return;

        if (res.status === 404) {
          try {
            sessionStorage.removeItem(`checkout:${checkoutId}:summary`);
          } catch {}
          navigate('/', { replace: true });
          return;
        }

        setCheckout(res.data as Checkout);
      } catch (e: any) {
        if (!ignore) setError(e?.response?.data?.message || '체크아웃 정보를 불러오지 못했습니다.');
      } finally {
        if (!ignore) setLoading(false);
      }
    }
    load();
    return () => {
      ignore = true;
    };
  }, [checkoutId, navigate]);

  // 요약 정보 복원 (state 우선, 없으면 sessionStorage)
  useEffect(() => {
    const s = (location.state as any) || null;
    if (s?.items) {
      console.log('PaymentPage - summary from state:', s);
      setSummary({ items: s.items, totalCount: s.totalCount, totalPrice: s.totalPrice });
      return;
    }
    if (checkoutId) {
      try {
        const raw = sessionStorage.getItem(`checkout:${checkoutId}:summary`);
        if (raw) {
          const parsed = JSON.parse(raw);
          console.log('PaymentPage - summary from sessionStorage:', parsed);
          setSummary(parsed);
        }
      } catch {}
    }
  }, [location.state, checkoutId]);

  // 할인 정보 로드
  useEffect(() => {
    if (user?.id) {
      fetchUserDiscounts();
    }
  }, [user]);

  // TossPayments 스크립트 로드
  useEffect(() => {
    if (window.TossPayments) {
      setTossReady(true);
      return;
    }
    const script = document.createElement('script');
    script.src = 'https://js.tosspayments.com/v2/standard';
    script.async = true;
    script.onload = () => setTossReady(true);
    document.body.appendChild(script);
    return () => {
      document.body.removeChild(script);
    };
  }, []);

  // TossPayments 위젯 초기 렌더 (한 번만)
  useEffect(() => {
    if (!tossReady || !checkout || !summary || !paymentMethodRef.current || !agreementRef.current) return;
    if (tossWidgetsRef.current) return; // 중복 렌더 방지

    const TossPayments = (window as any).TossPayments;
    const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY;
    // 로그인 사용자는 userId, 비회원은 고정값 사용
    const customerKey = user?.id ? `user-${user.id}` : 'guest-customer';

    const tossPayments = TossPayments(clientKey);
    const widgets = tossPayments.widgets({ customerKey });
    tossWidgetsRef.current = widgets;

    // 할인이 적용된 상품금액 + 배송비(무료배송 포함) 사용
    const paymentAmount = payableAmount;
    console.log('TossPayments 위젯 초기화 - (할인+배송비 반영) 최종 결제 금액:', paymentAmount);
    console.log('결제 금액 상세:', {
      productAmount,
      couponDiscount: couponDiscountAmount,
      pointsUsed: pointsDiscountAmount,
      shippingFee,
      finalAmount: paymentAmount
    });
    
    widgets.setAmount({
      currency: 'KRW',
      value: paymentAmount,
    });

    (async () => {
      await widgets.renderPaymentMethods({
        selector: '#payment-method',
        variantKey: 'DEFAULT',
      });
      await widgets.renderAgreement({
        selector: '#agreement',
        variantKey: 'AGREEMENT',
      });
    })();
  }, [tossReady, checkout, summary, payableAmount]);


  // 최종 금액이 바뀌면 위젯 금액 동기화
  useEffect(() => {
    if (!tossWidgetsRef.current) return;
    const paymentAmount = payableAmount;
    console.log('TossPayments 위젯 금액 업데이트 - (할인+배송비 반영):', paymentAmount);
    tossWidgetsRef.current.setAmount({
      currency: 'KRW',
      value: paymentAmount,
    });
  }, [payableAmount]);

  // 결제 버튼
  const handleTossPay = async () => {
    if (!tossWidgetsRef.current || !checkout) return;

    // 결제 추적을 위한 정보 로깅
    const paymentInfo = {
      orderId: checkout.orderId,
      productAmount,
      originalAmount: productAmount,
      couponDiscount: couponDiscountAmount,
      pointsUsed: pointsDiscountAmount,
      shippingFee,
      finalAmount: payableAmount,
      timestamp: new Date().toISOString(),
      userId: user?.id,
      selectedCoupon: selectedCoupon?.id,
      selectedCouponDetails: selectedCoupon?.coupon,
    };
    
    console.log('결제 진행 - 할인 적용 내역:', paymentInfo);
    
    // 결제 정보를 sessionStorage에 저장 (결제 완료 후 추적 가능)
    sessionStorage.setItem(`payment:${checkout.orderId}`, JSON.stringify(paymentInfo));
    
    // SuccessPage에서 백엔드로 전송할 할인 정보도 저장
    // 백엔드 호환: couponId 키 사용, 값이 없으면 discountInfo 자체를 저장하지 않음
    const hasCoupon = !!selectedCoupon?.id;
    const hasPoints = (paymentInfo.pointsUsed || 0) > 0;
    if (hasCoupon || hasPoints) {
  const discountInfo = {
    couponId: hasCoupon ? selectedCoupon!.id : undefined,
    couponDiscount: paymentInfo.couponDiscount,
    pointsUsed: paymentInfo.pointsUsed,
    originalAmount: paymentInfo.originalAmount,     // 상품금액(배송비 제외)
    shippingFee: shippingFee,                       // 추가
    finalAmount: payableAmount,                     // ✅ 배송비 포함 총 결제금액으로 변경
  };
  sessionStorage.setItem(`discount:${checkout.orderId}`, JSON.stringify(discountInfo));
} else {
  sessionStorage.removeItem(`discount:${checkout.orderId}`);
}

    try {
      // 쿠폰 id를 쿼리 파라미터로 successUrl에 포함
      const couponIdParam = selectedCoupon?.id ? `?couponId=${selectedCoupon.id}` : '';
      await tossWidgetsRef.current.requestPayment({
        orderId: checkout.orderId,
        orderName: summary?.items?.map((it) => it.productName).join(', ') || '주문',
        successUrl: window.location.origin + `/success${couponIdParam}`,
        failUrl: window.location.origin + '/fail',
        customerEmail: customerEmail || undefined,
        customerName: checkout.name,
        customerMobilePhone: checkout.phone,
      });
    } catch (error) {
      console.error('결제 요청 실패:', error);
    }
  };

  return (
    <div className="min-h-screen bg-gray-400">
      <div className="max-w-lg mx-auto px-4 py-24">
        <h2 className="text-2xl font-bold mb-6">결제</h2>

      {!checkoutId && (
        <p className="text-red-600">checkoutId가 없습니다. 이전 단계에서 다시 시도해주세요.</p>
      )}

      {loading ? (
        <div className="text-slate-500">로딩 중...</div>
      ) : error ? (
        <div className="text-red-600">{error}</div>
      ) : checkout ? (
        <div className="space-y-3">
          

          <div className="border rounded-lg p-4 bg-slate-50">
            <div className="font-semibold mb-2">주문 정보</div>
            <div className="flex justify-between text-sm"><span>수령인</span><span>{checkout.name}</span></div>
            <div className="flex justify-between text-sm"><span>연락처</span><span>{checkout.phone}</span></div>
            <div className="flex justify-between text-sm">
              <span>주소</span>
              <span className="text-right max-w-[60%] break-words">{checkout.address}</span>
            </div>
            <div className="flex justify-between text-sm"><span>총 수량</span><span>{checkout.quantity}개</span></div>

            {/* 화면 표시도 서버 금액 기준 */}
            {typeof checkout.amount === 'number' && (
              <div className="flex justify-between font-bold mt-2 text-lg">
                <span>총 결제 금액</span>
                <span>{clampKRW(checkout.amount).toLocaleString()}원</span>
              </div>
            )}
          </div>

          {/* 상품 요약: 표시는 가능하지만 금액 기준은 서버 amount */}
          {summary && summary.items?.length > 0 && (
            <div className="border rounded-lg p-4 bg-white">
              <div className="font-semibold mb-2">상품 내역</div>
              <ul className="space-y-1 text-sm">
                {summary.items.map((it, idx) => (
                  <li key={idx} className="flex justify-between">
                    <span>
                      {it.productName}
                      {it.size && <span className="text-gray-600"> ({it.size})</span>}
                      {" × "}{it.quantity}
                    </span>
                    <span>{(it.price * it.quantity).toLocaleString()}원</span>
                  </li>
                ))}
              </ul>
              <div className="mt-2 pt-2 border-t flex justify-between font-semibold">
                <span>총 수량</span>
                <span>{summary.totalCount}개</span>
              </div>
              <div className="flex justify-between text-sm text-slate-600">
                <span>원래 주문 금액</span>
                <span>{summary.totalPrice.toLocaleString()}원</span>
              </div>
            </div>
          )}

          {/* 할인 적용 섹션 */}
          {user && (
            <div className="border rounded-lg p-4 bg-white space-y-4">
              <div className="font-semibold">할인 혜택</div>
              
              {discountsLoading ? (
                <div className="text-center py-4 text-gray-500">할인 정보 로딩 중...</div>
              ) : (
                <>
                  {/* 포인트 사용 */}
                  <div className="space-y-2">
                    <div className="flex justify-between items-center">
                      <span className="text-sm font-medium">보유 포인트</span>
                      <span className="text-sm text-yellow-600 font-medium">{userPoints.toLocaleString()}P</span>
                    </div>
                    <div className="flex gap-2 items-center">
                      <input
                        type="number"
                        value={usePoints}
                        onChange={(e) => {
                          const value = parseInt(e.target.value) || 0;
                          const maxPointsBase = Math.max(0, productAmount - couponDiscountAmount);
                          const maxPoints = Math.min(userPoints, maxPointsBase);
                          setUsePoints(Math.max(0, Math.min(value, maxPoints)));
                        }}
                        placeholder="사용할 포인트"
                        className="flex-1 p-2 border rounded text-sm"
                        max={Math.min(userPoints, Math.max(0, productAmount - couponDiscountAmount))}
                        min="0"
                      />
                      <button
                        onClick={() => {
                          const maxPointsBase = Math.max(0, productAmount - couponDiscountAmount);
                          const maxPoints = Math.min(userPoints, maxPointsBase);
                          setUsePoints(maxPoints);
                        }}
                        className="px-3 py-2 bg-yellow-500 text-white rounded text-sm hover:bg-yellow-600"
                      >
                        전액사용
                      </button>
                    </div>
                  </div>

                  {/* 쿠폰 선택 */}
                  <div className="space-y-2">
                    <div className="flex justify-between items-center">
                      <span className="text-sm font-medium">보유 쿠폰</span>
                      <span className="text-sm text-blue-600">{Array.isArray(availableCoupons) ? availableCoupons.length : 0}개</span>
                    </div>
                    <select
                      value={selectedCoupon?.id || ""}
                      onChange={(e) => {
                        const raw = e.target.value;
                        if (!raw) {
                          setSelectedCoupon(null);
                          return;
                        }

                        const couponId = Number(raw);
                        const userCoupon = availableCoupons.find((c) => c.id === couponId) || null;
                        const orderAmount = summary?.totalPrice || 0;

                        if (userCoupon && !isCouponSelectable(userCoupon, orderAmount)) {
                          return;
                        }

                        console.log('쿠폰 선택:', { couponId, userCoupon });
                        setSelectedCoupon(userCoupon);
                      }}
                      className="w-full p-2 border rounded text-sm"
                    >
                      <option value="">쿠폰을 선택하세요</option>
                      {Array.isArray(availableCoupons) &&
                        availableCoupons.map((userCoupon) => {
                          const orderAmount = summary?.totalPrice || 0;
                          const disabled = !isCouponSelectable(userCoupon, orderAmount);
                          return (
                            <option key={userCoupon.id} value={userCoupon.id} disabled={disabled}>
                              {userCoupon.coupon.name} -
                              {userCoupon.coupon.type === 'PERCENTAGE'
                                ? ` ${userCoupon.coupon.discountValue}% 할인`
                                : ` ${userCoupon.coupon.discountValue.toLocaleString()}원 할인`}
                              {userCoupon.coupon.minOrderAmount && ` (${userCoupon.coupon.minOrderAmount.toLocaleString()}원 이상)`}
                              {disabled ? ' (조건 미충족)' : ''}
                            </option>
                          );
                        })}
                    </select>
                  </div>

                  {/* 할인 적용 내역 */}
                  <div className="space-y-1 pt-2 border-t">
                    <div className="flex justify-between text-sm">
                      <span>상품 금액</span>
                      <span>{productAmount.toLocaleString()}원</span>
                    </div>
                    {selectedCoupon && (
                      <div className="flex justify-between text-sm text-blue-600">
                        <span>쿠폰 할인</span>
                        <span>-{couponDiscountAmount.toLocaleString()}원</span>
                      </div>
                    )}
                    {pointsDiscountAmount > 0 && (
                      <div className="flex justify-between text-sm text-yellow-600">
                        <span>포인트 사용</span>
                        <span>-{pointsDiscountAmount.toLocaleString()}원</span>
                      </div>
                    )}
                    <div className="flex justify-between text-sm">
                      <span>배송비</span>
                      <span className={shippingFee === 0 ? 'text-green-600' : 'text-slate-700'}>
                        {shippingFee === 0 ? '무료' : `${shippingFee.toLocaleString()}원`}
                      </span>
                    </div>
                    <div className="flex justify-between font-bold text-lg pt-2 border-t">
                      <span>최종 결제 금액</span>
                      <span className="text-red-600">{payableAmount.toLocaleString()}원</span>
                    </div>

                    <div className="mt-2 text-xs text-slate-500 leading-relaxed">
                      배송비는 3,500원이며, 상품 금액(배송비 제외) 70,000원 이상 구매 시 무료배송입니다.
                      상품 금액이 70,000원 미만인 경우 배송비 3,500원이 추가되어 결제됩니다.
                      (상품 금액 + 배송비 합계가 70,000원을 넘더라도 무료배송이 적용되지 않습니다.)
                    </div>
                  </div>
                </>
              )}
            </div>
          )}

          {/* TossPayments 결제 UI */}
          <div ref={paymentMethodRef} id="payment-method" />
          <div ref={agreementRef} id="agreement" />

          <button
            onClick={handleTossPay}
            className="w-full py-2 rounded font-semibold bg-blue-700 text-white mt-6 disabled:opacity-50"
            disabled={!tossReady || !checkout}
          >
            결제하기
          </button>

          <div className="text-center">
            <a href="/" className="text-blue-700 underline">홈으로 돌아가기</a>
          </div>
        </div>
      ) : null}
      </div>
    </div>
  );
}
