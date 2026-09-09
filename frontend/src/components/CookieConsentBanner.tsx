import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { loadMetaPixel } from "../utils/metaPixel";
import { META_PIXEL_ID } from "../config/metaPixel";

const STORAGE_KEY = "cookieConsent";
type ConsentStatus = "granted" | "denied";

export default function CookieConsentBanner() {
  const [consent, setConsent] = useState<ConsentStatus | null>(null);
  const [checkedStorage, setCheckedStorage] = useState(false);

  // 저장된 동의 상태 확인 (최초 마운트 시 1회)
  useEffect(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === "granted" || stored === "denied") {
      setConsent(stored);
    }
    setCheckedStorage(true);
  }, []);

  // 동의 상태가 "granted"로 바뀔 때 메타 픽셀 로드
  useEffect(() => {
    if (consent === "granted") {
      loadMetaPixel(META_PIXEL_ID);
    }
  }, [consent]);

  const handleDecision = (decision: ConsentStatus) => {
    localStorage.setItem(STORAGE_KEY, decision);
    setConsent(decision);
  };

  if (!checkedStorage || consent !== null) return null;

  return (
    <div className="fixed bottom-0 left-0 right-0 z-[100] bg-black text-white px-4 py-4 flex flex-col sm:flex-row items-center justify-between gap-3">
      <p className="text-sm">
        이 사이트는 광고 성과 측정 및 맞춤형 광고 제공을 위해 쿠키를 사용하며, 동의 시 방문·이용
        기록이 메타(페이스북)에 제공될 수 있습니다. 자세한 내용은{" "}
        <Link to="/privacy-policy" className="underline">
          개인정보처리방침
        </Link>
        을 확인하세요.
      </p>
      <div className="flex gap-2 shrink-0">
        <button
          type="button"
          onClick={() => handleDecision("denied")}
          className="px-4 py-2 text-sm rounded border border-white/40 hover:bg-white/10"
        >
          거부
        </button>
        <button
          type="button"
          onClick={() => handleDecision("granted")}
          className="px-4 py-2 text-sm rounded bg-blue-600 hover:bg-blue-700"
        >
          동의
        </button>
      </div>
    </div>
  );
}
