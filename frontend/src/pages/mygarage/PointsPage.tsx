import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/useAuth";
import { API_BASE_URL } from "../../config/apiBase";

const SERVER = API_BASE_URL;

type PointsHistory = {
  id: number;
  userId: number;
  pointsChange: number; // + 또는 - 값
  reason: string;
  createdAt: string;
  remainingPoints: number;
};

type UserCoupon = {
  id: number; // userCoupon id
  available?: boolean;
  coupon?: {
    id: number;
    name?: string;
    code?: string;
    type?: string; // PERCENTAGE | AMOUNT
    discountValue?: number;
    minOrderAmount?: number;
    maxDiscountAmount?: number;
  };
  discountAmount?: number | null; // backend may calculate
  status?: string;
  obtainedAt?: string;
  usedAt?: string | null;
  usedOrderId?: string | null;
  validFrom?: string;
  validUntil?: string;
};

export default function PointsPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [currentPoints, setCurrentPoints] = useState<number>(0);
  const [pointsHistory, _setPointsHistory] = useState<PointsHistory[]>([]);
  const [loading, setLoading] = useState(true);
  const [userCoupons, setUserCoupons] = useState<UserCoupon[]>([]);
  const [couponsLoading, setCouponsLoading] = useState(false);
  const [couponsError, setCouponsError] = useState<string | null>(null);
  const [claimCode, setClaimCode] = useState("");
  const [claiming, setClaiming] = useState(false);

  useEffect(() => {
    if (!user) {
      navigate("/login");
      return;
    }
    
    fetchUserPoints();
    fetchUserCoupons();
  }, [user, navigate]);

  const fetchUserPoints = async () => {
    if (!user?.id) return;
    
    try {
      const token = localStorage.getItem("access_token");
      const response = await fetch(`${SERVER}/api/points/user/${user.id}`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      
      if (response.ok) {
        const data = await response.json();
        setCurrentPoints(data.points || 0);
      } else {
        console.error("포인트 조회 실패");
        setCurrentPoints(0);
      }
    } catch (error) {
      console.error("포인트 조회 중 오류:", error);
      setCurrentPoints(0);
    } finally {
      setLoading(false);
    }
  };

  const fetchUserCoupons = async () => {
    if (!user?.id) return;
    setCouponsLoading(true);
    setCouponsError(null);
    try {
      const token = localStorage.getItem("access_token");
      const res = await fetch(`${SERVER}/api/user-coupons/user/${user.id}/available`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body?.error || `HTTP ${res.status}`);
      }
      const data = await res.json();
      console.log('사용자 쿠폰 응답:', data);
      setUserCoupons(Array.isArray(data) ? data : []);
    } catch (e: any) {
      console.error('사용자 쿠폰 조회 실패', e);
      setCouponsError(e?.message || '쿠폰을 불러오지 못했습니다.');
      setUserCoupons([]);
    } finally {
      setCouponsLoading(false);
    }
  };

  return (
    <>
    <div className="hidden md:block fixed inset-0 overflow-hidden">
      {/* 배경 이미지 */}
      <div
        className="absolute inset-0 w-full h-full bg-cover bg-center"
        style={{
          backgroundImage: `url('${SERVER}/images/emptyload.webp')`,
          zIndex: 0,
        }}
      />

      <div className="relative max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 h-full flex items-center justify-center" style={{ zIndex: 1, paddingTop: "10vh" }}>
        <div
          className="relative bg-[#01132c] ml-6"
          style={{
            imageRendering: 'pixelated',
            clipPath: `polygon(
              0% 20px, 20px 20px, 20px 0%,
              calc(100% - 20px) 0%, calc(100% - 20px) 20px, 100% 20px,
              100% calc(100% - 20px), calc(100% - 20px) calc(100% - 20px), calc(100% - 20px) 100%,
              20px 100%, 20px calc(100% - 20px), 0% calc(100% - 20px)
            )`,
            padding: '20px',
            width: '80vw'
          }}
        >
          <div
            className="relative bg-[#03526a]"
            style={{
              imageRendering: 'pixelated',
              clipPath: `polygon(
                0% 18px, 18px 18px, 18px 0%,
                calc(100% - 18px) 0%, calc(100% - 18px) 18px, 100% 18px,
                100% calc(100% - 18px), calc(100% - 18px) calc(100% - 18px), calc(100% - 18px) 100%,
                18px 100%, 18px calc(100% - 18px), 0% calc(100% - 18px)
              )`,
              padding: '48px'
            }}
          >
            {/* 좌상단 타이틀 */}
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
  Coupon & Mileage 
</div>

            {/* 안쪽 컨텐츠 */}
            <div
              className="w-full px-4 py-4 bg-[#f2d4a7] scrollbar-hide overflow-y-auto"
              style={{
                imageRendering: 'pixelated',
                clipPath: `polygon(
                  0% 16px, 16px 16px, 16px 0%,
                  calc(100% - 16px) 0%, calc(100% - 16px) 16px, 100% 16px,
                  100% calc(100% - 16px), calc(100% - 16px) calc(100% - 16px), calc(100% - 16px) 100%,
                  16px 100%, 16px calc(100% - 16px), 0% calc(100% - 16px)
                )`,
                height: '52vh'
              }}
            >
              <div className="max-w-2xl w-full mx-auto px-4 sm:px-6 lg:px-8">
                {loading ? (
                  <div className="text-center py-12">
                    <p className="text-black text-base">로딩 중...</p>
                  </div>
                ) : (
                  <div className="space-y-6">
                    {/* 현재 적립금 */}
                    <div className="bg-white/10 rounded-lg border border-white/20 p-6">
                      <h2 className="text-xl font-bold text-black mb-4 font-beaver">
                        보유 적립금
                      </h2>
                      <div className="text-center">
                        <span className="text-4xl font-bold text-black">
                          {currentPoints.toLocaleString()}
                        </span>
                        <span className="text-xl text-black ml-2">P</span>
                      </div>
                    </div>

                    {/* 적립금 사용 안내 */}
                    <div className="bg-white/10 rounded-lg border border-white/20 p-4">
                      <h3 className="text-lg font-bold text-black mb-2">적립금 사용 안내</h3>
                      <ul className="text-sm text-black/80 space-y-1">
                        <li>• 1P = 1원으로 사용 가능합니다.</li>
                        <li>• 결제 시 보유 적립금 범위 내에서 사용할 수 있습니다.</li>
                        <li>• 적립금 사용 시 환불은 적립금으로 반환됩니다.</li>
                        <li>• 리뷰 작성 시 자동으로 적립금이 지급됩니다.</li>
                      </ul>
                    </div>

                    {/* 사용자 쿠폰 목록 */}
                    <div className="bg-white/10 rounded-lg border border-white/20 p-4">
                      <h3 className="text-lg font-bold text-black mb-2">사용 가능한 쿠폰</h3>

                      {/* 쿠폰 코드 입력 폼 */}
                      <div className="mb-3 flex gap-2">
                        <input
                          type="text"
                          placeholder="쿠폰 코드를 입력하세요"
                          value={claimCode}
                          onChange={(e) => setClaimCode(e.target.value)}
                          className="flex-1 p-2 border rounded"
                        />
                        <button
                          onClick={async () => {
                            if (!claimCode.trim()) return alert('쿠폰 코드를 입력해주세요.');
                            if (!user?.id) return alert('로그인이 필요합니다.');
                            setClaiming(true);
                            setCouponsError(null);
                            try {
                              const token = localStorage.getItem('access_token');
                              const res = await fetch(`${SERVER}/api/user-coupons/claim`, {
                                method: 'POST',
                                credentials: 'include',
                                headers: {
                                  'Content-Type': 'application/json',
                                  ...(token ? { Authorization: `Bearer ${token}` } : {}),
                                },
                                body: JSON.stringify({ couponCode: claimCode.trim() }),
                              });
                              const body = await res.json().catch(() => ({}));
                              if (!res.ok) {
                                throw new Error(body?.error || `HTTP ${res.status}`);
                              }
                              // 성공
                              alert('쿠폰이 정상적으로 등록되었습니다.');
                              setClaimCode('');
                              // 목록 갱신
                              await fetchUserCoupons();
                              console.log('claim coupon response:', body);
                            } catch (e: any) {
                              console.error('쿠폰 등록 실패', e);
                              setCouponsError(e?.message || '쿠폰 등록에 실패했습니다.');
                            } finally {
                              setClaiming(false);
                            }
                          }}
                          className="px-3 py-2 bg-blue-600 text-white rounded"
                          disabled={claiming}
                        >
                          {claiming ? '등록 중...' : '쿠폰 등록'}
                        </button>
                      </div>
                      {couponsLoading ? (
                        <div className="text-sm text-black/70">불러오는 중...</div>
                      ) : couponsError ? (
                        <div className="text-sm text-red-600">{couponsError}</div>
                      ) : userCoupons.length === 0 ? (
                        <div className="text-sm text-black/60">사용 가능한 쿠폰이 없습니다.</div>
                      ) : (
                        <ul className="space-y-2">
                          {userCoupons.map((uc) => (
                            <li key={uc.id} className="p-3 bg-white rounded">
                              <div className="flex justify-between items-start gap-4">
                                <div className="flex-1">
                                  <div className="font-semibold text-sm">{uc.coupon?.name ?? `쿠폰 #${uc.coupon?.id ?? uc.id}`}</div>
                                  {/* 쿠폰 코드 표시 제거 (요청에 따라 숨김) */}
                                  {/** description may or may not exist on coupon object */}
                                  {uc.coupon && (uc.coupon as any).description && (
                                    <div className="text-xs text-gray-700 mt-1">{(uc.coupon as any).description}</div>
                                  )}
                                  <div className="text-xs text-gray-600 mt-1 space-y-0.5">
                                    <div>상태: {uc.status ?? (uc.available ? 'AVAILABLE' : 'UNAVAILABLE')}</div>
                                    {uc.coupon?.minOrderAmount && (
                                      <div>최소 주문금액: {uc.coupon.minOrderAmount.toLocaleString()}원 이상</div>
                                    )}
                                    {uc.coupon?.maxDiscountAmount && (
                                      <div>최대 할인금액: {uc.coupon.maxDiscountAmount.toLocaleString()}원</div>
                                    )}
                                    {uc.validUntil && (
                                      <div>유효기간: ~{new Date(uc.validUntil).toLocaleDateString()}</div>
                                    )}
                                  </div>
                                </div>
                                <div className="flex flex-col items-end">
                                  <div className="text-sm font-bold">
                                    {(() => {
                                      const val = uc.discountAmount ?? uc.coupon?.discountValue;
                                      if (val == null) return '-';
                                      return uc.coupon?.type === 'PERCENTAGE' ? `${val}%` : `${val.toLocaleString()}원`;
                                    })()}
                                  </div>
                                </div>
                              </div>
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>

                    {/* 적립금 내역이 있다면 여기에 표시 */}
                    {pointsHistory.length > 0 && (
                      <div className="bg-white/10 rounded-lg border border-white/20 p-4">
                        <h3 className="text-lg font-bold text-black mb-4">적립금 내역</h3>
                        <div className="space-y-2">
                          {pointsHistory.map((history) => (
                            <div key={history.id} className="flex justify-between items-center py-2 border-b border-white/10">
                              <div>
                                <p className="text-sm font-medium text-black">{history.reason}</p>
                                <p className="text-xs text-black/60">
                                  {new Date(history.createdAt).toLocaleDateString('ko-KR')}
                                </p>
                              </div>
                              <div className="text-right">
                                <p className={`text-sm font-bold ${history.pointsChange > 0 ? 'text-blue-600' : 'text-red-600'}`}>
                                  {history.pointsChange > 0 ? '+' : ''}{history.pointsChange.toLocaleString()}P
                                </p>
                                <p className="text-xs text-black/60">
                                  잔액: {history.remainingPoints.toLocaleString()}P
                                </p>
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    {/* Mobile: md 미만 단순화된 포인트 UI */}
    <div
      className="block md:hidden min-h-screen text-white font-sans"
      style={{
        backgroundImage: `url('${SERVER}/images/emptyload.webp')`,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
      }}
    >
      <div className="px-5 pb-10 space-y-3 overflow-y-auto pt-20">
        <div className="bg-black/50 rounded-xl p-4 space-y-4 mt-30">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => navigate("/mygarage")}
              aria-label="마이차고로 이동"
              className="h-9 w-9 flex items-center justify-center rounded bg-white/10 hover:bg-white/20 transition-colors"
            >
              <span className="text-2xl leading-none">←</span>
            </button>
            <h2 className="text-xl font-bold">Coupon & Mileage</h2>
          </div>

          {loading ? (
            <div className="text-white/80">로딩 중...</div>
          ) : (
            <div className="space-y-3">
              <div className="p-4 bg-white/10 rounded-lg text-center">
                <div className="text-sm text-white/80">보유 적립금</div>
                <div className="text-3xl font-bold text-white">{currentPoints.toLocaleString()}P</div>
              </div>

              <div className="p-3 bg-white/10 rounded-lg">
                <div className="mb-2 text-white font-semibold">쿠폰 등록</div>
                <div className="flex gap-2">
                  <input value={claimCode} onChange={(e) => setClaimCode(e.target.value)} placeholder="쿠폰 코드" className="flex-1 p-2 rounded bg-white/10 text-white" />
                  <button
                    onClick={async () => {
                      if (!claimCode.trim()) return alert('쿠폰 코드를 입력해주세요.');
                      if (!user?.id) return alert('로그인이 필요합니다.');
                      setClaiming(true);
                      setCouponsError(null);
                      try {
                        const token = localStorage.getItem('access_token');
                        const res = await fetch(`${SERVER}/api/user-coupons/claim`, {
                          method: 'POST',
                          credentials: 'include',
                          headers: {
                            'Content-Type': 'application/json',
                            ...(token ? { Authorization: `Bearer ${token}` } : {}),
                          },
                          body: JSON.stringify({ couponCode: claimCode.trim() }),
                        });
                        const body = await res.json().catch(() => ({}));
                        if (!res.ok) throw new Error(body?.error || `HTTP ${res.status}`);
                        alert('쿠폰이 정상적으로 등록되었습니다.');
                        setClaimCode('');
                        await fetchUserCoupons();
                      } catch (e: any) {
                        console.error('쿠폰 등록 실패', e);
                        setCouponsError(e?.message || '쿠폰 등록에 실패했습니다.');
                      } finally {
                        setClaiming(false);
                      }
                    }}
                    className="px-3 py-2 rounded bg-blue-600 text-white"
                    disabled={claiming}
                  >
                    {claiming ? '등록 중...' : '등록'}
                  </button>
                </div>
                {couponsLoading ? <div className="text-sm text-white/80 mt-2">불러오는 중...</div> : couponsError ? <div className="text-sm text-red-400 mt-2">{couponsError}</div> : null}
              </div>

              <div>
                <h3 className="text-sm font-semibold text-white mb-2">사용 가능한 쿠폰</h3>
                {userCoupons.length === 0 ? (
                  <div className="text-white/80">사용 가능한 쿠폰이 없습니다.</div>
                ) : (
                  <div className="space-y-2">
                    {userCoupons.map((uc) => (
                      <div key={uc.id} className="p-3 bg-white/10 rounded space-y-1">
                        <div className="flex justify-between">
                          <div className="text-white text-sm font-semibold">{uc.coupon?.name ?? `쿠폰 #${uc.coupon?.id ?? uc.id}`}</div>
                          <div className="text-white text-sm font-bold">{(() => { const val = uc.discountAmount ?? uc.coupon?.discountValue; if (val == null) return '-'; return uc.coupon?.type === 'PERCENTAGE' ? `${val}%` : `${val.toLocaleString()}원`; })()}</div>
                        </div>
                        <div className="text-xs text-white/70 space-y-0.5">
                          {uc.coupon?.minOrderAmount && (
                            <div>최소 주문금액: {uc.coupon.minOrderAmount.toLocaleString()}원 이상</div>
                          )}
                          {uc.coupon?.maxDiscountAmount && (
                            <div>최대 할인금액: {uc.coupon.maxDiscountAmount.toLocaleString()}원</div>
                          )}
                          {uc.validUntil && (
                            <div>유효기간: ~{new Date(uc.validUntil).toLocaleDateString()}</div>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
    </>
  );
}