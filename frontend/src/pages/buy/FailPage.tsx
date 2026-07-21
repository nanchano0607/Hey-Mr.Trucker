import { useNavigate} from "react-router-dom";

export default function FailPage() {
  
  const navigate = useNavigate();

  
  

  return (
    <div className="relative min-h-[100svh] overflow-hidden">
      {/* 배경 */}
      <div
        className="absolute inset-0 bg-cover bg-center"
        style={{
          backgroundImage: `radial-gradient(80% 60% at 50% 10%, rgba(255,255,255,0.10), rgba(0,0,0,0)),
                           linear-gradient(180deg, #071225 0%, #020714 55%, #020714 100%)`,
        }}
      />
      <div className="absolute inset-0 opacity-[0.10] pointer-events-none bg-[linear-gradient(to_right,rgba(255,255,255,0.08)_1px,transparent_1px),linear-gradient(to_bottom,rgba(255,255,255,0.08)_1px,transparent_1px)] bg-[size:28px_28px]" />

      <div className="relative z-10 mx-auto flex min-h-[100svh] max-w-3xl items-center justify-center px-4 py-10">
        {/* 픽셀 프레임 */}
        <div
          className="w-full"
          style={{ imageRendering: "pixelated" }}
        >
          <div
            className="relative bg-[#01132c]"
            style={{
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
                clipPath: `polygon(
                  0% 18px, 18px 18px, 18px 0%,
                  calc(100% - 18px) 0%, calc(100% - 18px) 18px, 100% 18px,
                  100% calc(100% - 18px), calc(100% - 18px) calc(100% - 18px), calc(100% - 18px) 100%,
                  18px 100%, 18px calc(100% - 18px), 0% calc(100% - 18px)
                )`,
                padding: "28px",
              }}
            >
              {/* 헤더 */}
              <div className="relative mb-6 pb-4 border-b border-white/15 text-center">
                <div className="text-xs tracking-[0.30em] text-white/70">
                  HEY! MR TRUCKER CHECKOUT
                </div>
                <h1 className="mt-1 text-white font-beaver text-2xl md:text-3xl font-bold">
                  PAYMENT FAILED
                </h1>
              </div>

              {/* 콘텐츠 */}
              <div
                className="bg-[#f2d4a7] px-5 py-8 md:px-7 text-center"
                style={{
                  clipPath: `polygon(
                    0% 16px, 16px 16px, 16px 0%,
                    calc(100% - 16px) 0%, calc(100% - 16px) 16px, 100% 16px,
                    100% calc(100% - 16px), calc(100% - 16px) calc(100% - 16px), calc(100% - 16px) 100%,
                    16px 100%, 16px calc(100% - 16px), 0% calc(100% - 16px)
                  )`,
                }}
              >
                {/* 실패 라벨 */}
                <div className="inline-flex items-center gap-2 rounded px-3 py-2 bg-[#01132c] text-white mb-4">
                  <span className="text-lg leading-none">✖</span>
                  <span className="font-beaver text-xl">ERROR</span>
                </div>

                <p className="text-[#01132c] font-bold text-lg mb-2">
                  오류가 발생했습니다
                </p>
                <p className="text-[#01132c]/80 text-sm mb-6">
                  결제 처리 중 문제가 발생했습니다.<br />
                  잠시 후 다시 시도해 주세요.
                </p>

                <div className="flex flex-col sm:flex-row gap-3 justify-center">
                  <button
                    onClick={() => navigate("/cart")}
                    className="px-6 py-3 bg-[#03526a] text-white rounded font-bold hover:opacity-95 transition-opacity"
                  >
                    다시 시도하기
                  </button>
                  <button
                    onClick={() => navigate("/")}
                    className="px-6 py-3 bg-white/70 text-[#01132c] rounded font-bold hover:bg-white transition-colors border border-black/10"
                  >
                    홈으로
                  </button>
                </div>
              </div>

              {/* 하단 문구 */}
              <div className="mt-4 text-center text-white/60 text-xs">
                문제가 반복되면 고객센터로 문의해주세요.
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
