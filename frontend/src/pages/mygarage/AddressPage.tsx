import { useState, useEffect, useRef } from "react";
import { useAuth } from "../../auth/useAuth";
import { useNavigate } from "react-router-dom";
import { API_BASE_URL } from "../../config/apiBase";

const SERVER = API_BASE_URL;

export default function AddressPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [addresses, setAddresses] = useState<string[]>([]);
  const [newAddress, setNewAddress] = useState("");
  const [addressDetail, setAddressDetail] = useState("");

  // 스크롤바 숨기기 위한 스타일 추가
  useEffect(() => {
    const style = document.createElement('style');
    style.textContent = `
      .scrollbar-hide::-webkit-scrollbar {
        display: none;
      }
    `;
    document.head.appendChild(style);
    return () => {
      document.head.removeChild(style);
    };
  }, []);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);
  
  const detailRef = useRef<HTMLInputElement | null>(null);
  const popupRef = useRef<Window | null>(null);

  // 주소 목록 조회
  useEffect(() => {
    if (user?.id) {
      fetchAddresses();
    }
  }, [user]);

  // postMessage 수신 (도로명 주소 API)
  useEffect(() => {
    function onMessage(e: MessageEvent) {
      const payload = e.data as any;
      if (!payload) return;

      const full =
        payload.fullAddress ||
        payload.roadAddr ||
        payload.roadAddrPart1 ||
        payload.jibunAddr ||
        "";

      if (full) {
        setNewAddress(full);
        setTimeout(() => {
          detailRef.current?.focus();
        }, 50);
      }

      if (popupRef.current && !popupRef.current.closed) popupRef.current.close();
      popupRef.current = null;
    }
    window.addEventListener("message", onMessage);
    return () => window.removeEventListener("message", onMessage);
  }, []);

  const fetchAddresses = async () => {
    if (!user?.id) return;
    try {
      const token = localStorage.getItem("access_token");
      const response = await fetch(`${SERVER}/api/user/${user.id}/addresses`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (response.ok) {
        const data = await response.json();
        setAddresses(data);
      }
    } catch (error) {
      console.error("주소 조회 실패:", error);
    }
  };

  // 주소 추가
  const handleAddAddress = async () => {
    const fullAddress = addressDetail ? `${newAddress} ${addressDetail}` : newAddress;
    
    if (!fullAddress.trim()) {
      showMessage("error", "주소를 입력해주세요.");
      return;
    }

    setLoading(true);
    try {
      const token = localStorage.getItem("access_token");
      const response = await fetch(`${SERVER}/api/user/${user?.id}/addresses`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ address: fullAddress }),
      });

      const data = await response.json();
      if (response.ok) {
        showMessage("success", data.message || "주소가 추가되었습니다.");
        setNewAddress("");
        setAddressDetail("");
        fetchAddresses();
      } else {
        showMessage("error", data.error || "주소 추가에 실패했습니다.");
      }
    } catch (error) {
      showMessage("error", "오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // 주소 검색 팝업 열기
  const openJusoPopup = () => {
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

  // 주소 삭제
  const handleRemoveAddress = async (address: string) => {
    if (!confirm(`"${address}"를 삭제하시겠습니까?`)) return;

    setLoading(true);
    try {
      const token = localStorage.getItem("access_token");
      const response = await fetch(`${SERVER}/api/user/${user?.id}/addresses/remove`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ address }),
      });

      const data = await response.json();
      if (response.ok) {
        showMessage("success", data.message || "주소가 삭제되었습니다.");
        fetchAddresses();
      } else {
        showMessage("error", data.error || "주소 삭제에 실패했습니다.");
      }
    } catch (error) {
      showMessage("error", "오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const showMessage = (type: "success" | "error", text: string) => {
    setMessage({ type, text });
    setTimeout(() => setMessage(null), 3000);
  };

  return (
    <>
    {/* Desktop: md 이상에서 표시 */}
    <div className="hidden md:block fixed inset-0 overflow-hidden">
      {/* 배경 이미지 - fixed로 고정 */}
      {/* 📦 배경 레이어: 화면 전체를 덮는 고정 배경 (inset-0 = top:0, right:0, bottom:0, left:0) */}
      <div
        className="fixed inset-0 w-full h-full bg-cover bg-center"
        style={{
          backgroundImage: `url('${SERVER}/images/emptyload.webp')`,
          zIndex: 0,
        }}
      />

      {/* 📦 메인 컨텐츠 컨테이너: 최대 너비 2xl(42rem = 672px), 중앙 정렬, 화면 중앙 배치 */}
      {/* h-full = 전체 높이, flex items-center = 세로 중앙 정렬 */}
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
          {/* 중간 테두리 (#1a5f7a) */}
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
            {/* 왼쪽 위 글씨 */}
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
  Address
</div>
            
            {/* 가장 안쪽 컨텐츠 (#F5DEB3) */}
            <div 
              className="w-full px-4 bg-[#f2d4a7] scrollbar-hide overflow-y-auto"
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
        <div className="max-w-2xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-4">
        {/* 메시지 알림 */}
        {/* 📦 알림 박스: mb-4 = 하단 여백 16px, p-4 = 내부 패딩 16px */}
        {message && (
          <div
            className={`mb-4 p-4 rounded-lg ${
              message.type === "success" ? "bg-green-500/90" : "bg-red-500/90"
            } text-white`}
          >
            {message.text}
          </div>
        )}

        {/* 주소 추가 폼 */}
        {/* 📦 폼 컨테이너: 투명 배경(bg-transparent), 내부 패딩 24px(p-6), 하단 여백 24px(mb-6) */}
        <div className="bg-transparent p-2 rounded-lg mb-2">
          {/* 📝 폼 제목: 텍스트 크기 xl(20px), 하단 여백 16px */}
          <h2 className="text-xl font-bold mb-4 text-black">New Address</h2>
          {/* 📦 입력 필드 그룹: flex로 가로 배치, gap-2 = 요소 간 간격 8px, mb-3 = 하단 여백 12px */}
          <div className="flex gap-2 mb-3">
            {/* 📦 주소 입력 필드: flex-1 = 남은 공간 모두 차지, p-3 = 내부 패딩 12px */}
            <input
              type="text"
              value={newAddress}
              readOnly
              onClick={openJusoPopup}
              placeholder="주소검색 버튼을 눌러 선택하세요"
              className="flex-1 p-3 border border-white/30 rounded-lg bg-white/10 text-black placeholder-gray-500 cursor-pointer"
              title="주소는 검색으로만 입력됩니다"
            />
            {/* 📦 주소검색 버튼: px-6 = 좌우 패딩 24px, py-3 = 상하 패딩 12px */}
            <button
              type="button"
              onClick={openJusoPopup}
              className="px-6 py-3 bg-white/20 text-black rounded-lg font-bold hover:bg-white/30 transition-colors border border-white/30"
            >
              주소검색
            </button>
          </div>
          {/* 📦 상세주소 입력 필드: w-full = 너비 100%, p-3 = 내부 패딩 12px, mb-3 = 하단 여백 12px */}
          <input
            ref={detailRef}
            type="text"
            value={addressDetail}
            onChange={(e) => setAddressDetail(e.target.value)}
            placeholder="상세주소 (동/층/호)"
            className="w-full p-3 border border-white/30 rounded-lg bg-white/10 text-black placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-white/50 mb-3"
            onKeyPress={(e) => e.key === "Enter" && handleAddAddress()}
            disabled={loading}
          />
          {/* 📦 추가 버튼: w-full = 너비 100%, 투명 배경, 테두리만 */}
          <button
            onClick={handleAddAddress}
            disabled={loading}
            className="w-full px-6 py-3 bg-transparent text-black rounded-lg font-bold border-2 border-white hover:bg-white/10 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {loading ? "처리중..." : "Save"}
          </button>
        </div>

        {/* 주소 목록 */}
        {/* 📦 목록 컨테이너: 투명 배경(bg-transparent), 내부 패딩 24px(p-6), 고정 높이 */}
        <div className="bg-transparent p-6 rounded-lg">
          {/* 📝 목록 제목: 텍스트 크기 xl(20px), 하단 여백 16px */}
          <h2 className="text-xl font-bold mb-4 text-black">My Address</h2>
          {/* 📦 스크롤 컨테이너: h-60(240px) 고정 높이, overflow-y-auto로 세로 스크롤, 스크롤바 숨김 */}
          <div className="h-30 overflow-y-auto pr-2 scrollbar-hide" style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}>
            {addresses.length === 0 ? (
              <p className="text-black">등록된 주소가 없습니다.</p>
            ) : (
              // 📦 주소 아이템 리스트: space-y-2 = 각 아이템 간 세로 간격 8px
              <div className="space-y-2">
                {addresses.map((address, index) => (
                  // 📦 주소 아이템: flex로 가로 배치, p-4 = 내부 패딩 16px
                  <div
                    key={index}
                    className="flex items-center justify-between p-4 bg-white/10 rounded-lg border border-white/20"
                  >
                    <span className="text-black">{address}</span>
                    {/* 📦 삭제 버튼: 작은 크기, px-3 = 좌우 패딩 12px, py-1 = 상하 패딩 4px */}
                    <button
                      onClick={() => handleRemoveAddress(address)}
                      disabled={loading}
                      className="px-3 py-1 bg-transparent text-red-600 rounded border border-red-600 hover:bg-red-400/10 disabled:opacity-50 transition-colors text-xs font-semibold"
                    >
                      Delete
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
            </div>
          </div>
        </div>
        </div>
      </div>
    </div>

    {/* Mobile: md 미만에서 표시되는 단순화된 주소 UI */}
    <div
      className="block md:hidden min-h-screen text-white font-sans"
      style={{
        backgroundImage: `url('${SERVER}/images/emptyload.webp')`,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
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
            <h2 className="text-xl font-bold">Address</h2>
          </div>

          <div className="space-y-2">
            <input
              type="text"
              value={newAddress}
              readOnly
              onClick={openJusoPopup}
              placeholder="주소검색"
              className="w-full p-3 rounded-lg bg-white/10 text-black placeholder-gray-500 cursor-pointer"
              title="주소는 검색으로만 입력됩니다"
            />

            <input
              ref={detailRef}
              type="text"
              value={addressDetail}
              onChange={(e) => setAddressDetail(e.target.value)}
              placeholder="상세주소 (동/층/호)"
              className="w-full p-3 rounded-lg bg-white/10 text-black placeholder-gray-500 focus:outline-none"
              onKeyPress={(e) => e.key === 'Enter' && handleAddAddress()}
              disabled={loading}
            />

            <button
              onClick={handleAddAddress}
              disabled={loading}
              className="w-full h-12 rounded-md bg-transparent border-2 border-white text-white font-bold disabled:opacity-50"
            >
              {loading ? '처리중...' : 'Save'}
            </button>
          </div>

          <div className="pt-2">
            <h3 className="text-lg font-bold mb-2">My Address</h3>
            <div className="space-y-2">
              {addresses.length === 0 ? (
                <p className="text-white">등록된 주소가 없습니다.</p>
              ) : (
                addresses.map((address, index) => (
                  <div key={index} className="flex items-center justify-between p-3 bg-white/10 rounded">
                    <span className="text-white text-sm">{address}</span>
                    <button
                      onClick={() => handleRemoveAddress(address)}
                      disabled={loading}
                      className="px-3 py-1 bg-transparent text-red-400 rounded border border-red-400 hover:bg-red-400/10 disabled:opacity-50 text-xs font-semibold"
                    >
                      Delete
                    </button>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>
    </div>

    </>
  );
}