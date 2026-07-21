import { useEffect, useState, useRef, useMemo } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import api from "../../lib/axios";

export default function SuccessPage() {
  const [searchParams] = useSearchParams();
  const qp = useMemo(() => searchParams.toString(), [searchParams]);
  const navigate = useNavigate();

  const [status, setStatus] = useState<"loading" | "success" | "error">("loading");
  const [message, setMessage] = useState("");
  const [orderIdResult, setOrderIdResult] = useState<number | null>(null);

  const hasConfirmed = useRef(false);

  useEffect(() => {
    if (hasConfirmed.current) return;
    hasConfirmed.current = true;

    const sp = new URLSearchParams(qp);
    const paymentKey = sp.get("paymentKey");
    const orderId = sp.get("orderId");
    let amount = sp.get("amount");

    if (!paymentKey || !orderId || !amount) {
      setStatus("error");
      setMessage("결제 정보가 누락되었습니다.");
      return;
    }

    (async () => {
      try {
        const paymentInfoStr = sessionStorage.getItem(`payment:${orderId}`);
        const discountInfoStr = sessionStorage.getItem(`discount:${orderId}`);
        if (paymentInfoStr) {
          const paymentInfo = JSON.parse(paymentInfoStr);
          amount = String(paymentInfo.finalAmount);
        }

        const requestData: any = { paymentKey, orderId, amount };
        if (discountInfoStr) requestData.discountInfo = JSON.parse(discountInfoStr);

        const res = await api.post(`/api/orders/confirm`, requestData);

        setOrderIdResult(res.data.orderId || res.data.id);
        setStatus("success");
        setMessage("결제가 완료되었습니다!");

        sessionStorage.removeItem(`payment:${orderId}`);
        sessionStorage.removeItem(`discount:${orderId}`);
        sessionStorage.removeItem(`checkout:${orderId}:summary`);
      } catch (err: any) {
        console.error("결제 승인 오류:", err?.response?.data || err);
        setStatus("error");
        setMessage(
          err?.response?.data?.error ||
            err?.response?.data?.message ||
            "결제 승인 중 오류가 발생했습니다."
        );
      }
    })();
  }, [qp]);

  // ===== 디자인 공통 래퍼 (배경 + 픽셀 패널) =====
  const Shell = ({ children }: { children: React.ReactNode }) => (
    <div className="relative min-h-[100svh] overflow-hidden">
      {/* 배경 (원하면 YOUR_BG로 교체) */}
      <div
        className="absolute inset-0 bg-cover bg-center"
        style={{
          backgroundImage: `radial-gradient(80% 60% at 50% 10%, rgba(255,255,255,0.10), rgba(0,0,0,0)),
                           linear-gradient(180deg, #071225 0%, #020714 55%, #020714 100%)`,
        }}
      />
      {/* 약한 노이즈/그리드 느낌 */}
      <div className="absolute inset-0 opacity-[0.10] pointer-events-none bg-[linear-gradient(to_right,rgba(255,255,255,0.08)_1px,transparent_1px),linear-gradient(to_bottom,rgba(255,255,255,0.08)_1px,transparent_1px)] bg-[size:28px_28px]" />

      <div className="relative z-10 mx-auto flex min-h-[100svh] max-w-3xl items-center justify-center px-4 py-10">
        {/* 픽셀 프레임 패널 */}
        <div
          className="w-full"
          style={{
            imageRendering: "pixelated",
          }}
        >
          <div
            className="relative bg-[#01132c]"
            style={{
              imageRendering: "pixelated",
              clipPath: `polygon(
                0% 20px, 20px 20px, 20px 0%,
                calc(100% - 20px) 0%, calc(100% - 20px) 20px, 100% 20px,
                100% calc(100% - 20px), calc(100% - 20px) calc(100% - 20px), calc(100% - 20px) 100%,
                20px 100%, 20px calc(100% - 20px), 0% calc(100% - 20px)
              )`,
              padding: "18px",
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
                padding: "28px",
              }}
            >
              {/* 헤더 바: 좌측 버튼 + 중앙 타이틀 */}
              <div className="relative mb-6 pb-4 border-b border-white/15">
                <button
                  type="button"
                  onClick={() => navigate("/")}
                  aria-label="홈으로 이동"
                  className="absolute left-0 top-1/2 -translate-y-1/2 h-9 w-9 flex items-center justify-center rounded bg-white/10 hover:bg-white/20 transition-colors"
                >
                  <span className="text-2xl leading-none text-white">←</span>
                </button>

                <div className="text-center">
                  <div className="text-xs tracking-[0.30em] text-white/70">
                    HEY! MR TRUCKER CHECKOUT
                  </div>
                  <h1 className="mt-1 text-white font-beaver text-2xl md:text-3xl font-bold">
                    PAYMENT RESULT
                  </h1>
                </div>
              </div>

              {/* 콘텐츠 영역 */}
              <div
                className="bg-[#f2d4a7] px-5 py-6 md:px-7"
                style={{
                  imageRendering: "pixelated",
                  clipPath: `polygon(
                    0% 16px, 16px 16px, 16px 0%,
                    calc(100% - 16px) 0%, calc(100% - 16px) 16px, 100% 16px,
                    100% calc(100% - 16px), calc(100% - 16px) calc(100% - 16px), calc(100% - 16px) 100%,
                    16px 100%, 16px calc(100% - 16px), 0% calc(100% - 16px)
                  )`,
                }}
              >
                {children}
              </div>

              {/* 하단 라벨 */}
              <div className="mt-4 flex items-center justify-between text-white/70 text-xs">
                <span className="tracking-widest">ROUTE 99</span>
                <span className="tracking-widest">SAFE DRIVE</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  if (status === "loading") {
    return (
      <Shell>
        <div className="flex flex-col items-center justify-center py-10">
          {/* 픽셀 로더 느낌 */}
          <div className="mb-4 grid grid-cols-3 gap-2">
            <div className="h-3 w-3 bg-[#01132c]/70 animate-pulse" />
            <div className="h-3 w-3 bg-[#01132c]/70 animate-pulse [animation-delay:120ms]" />
            <div className="h-3 w-3 bg-[#01132c]/70 animate-pulse [animation-delay:240ms]" />
          </div>
          <h2 className="text-[#01132c] font-beaver text-2xl font-bold">
            결제 처리 중...
          </h2>
          <p className="mt-2 text-[#01132c]/70 text-sm">
            잠시만 기다려주세요. 결제 승인 요청을 처리하고 있습니다.
          </p>
        </div>
      </Shell>
    );
  }

  if (status === "error") {
    return (
      <Shell>
        <div className="py-2">
          <div className="inline-flex items-center gap-2 rounded px-3 py-2 bg-[#01132c] text-white">
            <span className="text-lg leading-none">✖</span>
            <span className="font-beaver text-xl">PAYMENT FAILED</span>
          </div>

          <div className="mt-5 rounded bg-white/60 border border-black/10 p-4">
            <p className="text-[#01132c] font-bold">실패 사유</p>
            <p className="mt-2 text-[#01132c]/80 text-sm whitespace-pre-line">
              {message}
            </p>
          </div>

          <div className="mt-6 flex flex-col sm:flex-row gap-3">
            <button
              onClick={() => navigate("/")}
              className="w-full sm:w-auto px-5 py-3 bg-[#01132c] text-white rounded font-bold hover:opacity-90 transition-opacity"
            >
              홈으로 돌아가기
            </button>
            <button
              onClick={() => navigate("/cart")}
              className="w-full sm:w-auto px-5 py-3 bg-white/70 text-[#01132c] rounded font-bold hover:bg-white transition-colors border border-black/10"
            >
              장바구니로
            </button>
          </div>
        </div>
      </Shell>
    );
  }

  // success
  return (
    <Shell>
      <div className="py-2">
        <div className="inline-flex items-center gap-2 rounded px-3 py-2 bg-[#01132c] text-white">
          <span className="text-lg leading-none">✔</span>
          <span className="font-beaver text-xl">PAYMENT COMPLETE</span>
        </div>

        <div className="mt-5 rounded bg-white/60 border border-black/10 p-4">
          <p className="text-[#01132c] font-bold">상태</p>
          <p className="mt-2 text-[#01132c]/80 text-sm">{message}</p>

          {orderIdResult && (
            <div className="mt-4 flex items-center justify-between rounded bg-white/60 border border-black/10 px-3 py-2">
              <span className="text-[#01132c] font-bold text-sm">주문번호</span>
              <span className="text-[#01132c] font-mono text-sm">#{orderIdResult}</span>
            </div>
          )}
        </div>

        <div className="mt-6 grid grid-cols-1 sm:grid-cols-2 gap-3">
          <button
            onClick={() => navigate("/order")}
            className="px-5 py-3 bg-[#03526a] text-white rounded font-bold hover:opacity-95 transition-opacity"
          >
            주문 내역 보기
          </button>
          <button
            onClick={() => navigate("/")}
            className="px-5 py-3 bg-white/70 text-[#01132c] rounded font-bold hover:bg-white transition-colors border border-black/10"
          >
            홈으로
          </button>
        </div>

        <p className="mt-5 text-xs text-[#01132c]/60">
          결제 완료 후 주문 상태 반영까지 수 초가 걸릴 수 있습니다.
        </p>
      </div>
    </Shell>
  );
}
