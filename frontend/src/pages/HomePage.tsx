import { useRef, useEffect, useState } from "react";
import { isMobileDevice } from "../utils/isMobile"; // 네가 따로 만든 파일
import { API_BASE_URL } from "../config/apiBase";

const SERVER = API_BASE_URL;

function isVideo(url: string) {
  const clean = (url || "").split(/[?#]/)[0].toLowerCase();
  return clean.endsWith(".mp4") || clean.endsWith(".webm") || clean.endsWith(".ogg");
}

export default function HomePage() {
  const [bgUrl, setBgUrl] = useState("");
  const [videoEnded, setVideoEnded] = useState(false);
  const [videoError, setVideoError] = useState(false);
  const [isMobile, setIsMobile] = useState(false);

  const videoRef = useRef<HTMLVideoElement>(null);

  // 비디오 끝나면/에러나면/모바일에서는 보여줄 기본 이미지
  const fallbackImage = `${SERVER}/uploads/cap/background.webp`;
  const mobileMainImage = `${SERVER}/uploads/cap/mobileMainLoad.webp`;

  // 모바일 감지 (한 번만)
  useEffect(() => {
    setIsMobile(isMobileDevice());
  }, []);

  // 배경 리소스 가져오기
  useEffect(() => {
    fetch(`${SERVER}/api/background`)
      .then((res) => res.json())
      .then((data) => setBgUrl(data?.url ?? ""))
      .catch(() => setBgUrl(""));
  }, []);

  // PC에서만 비디오 자동 재생
  useEffect(() => {
    if (isMobile) return;
    if (!bgUrl || !isVideo(bgUrl)) return;

    const el = videoRef.current;
    if (!el) return;

    el.muted = true;
    el.playsInline = true;
    el.preload = "auto";

    const tryPlay = async () => {
      try {
        await el.play();
      } catch {
        setTimeout(() => el.play().catch(() => {}), 100);
      }
    };
    tryPlay();
  }, [bgUrl, isMobile]);

  const handleVideoEnd = () => setVideoEnded(true);
  const handleVideoError = () => {
    setVideoError(true);
    setVideoEnded(true);
  };

  const showVideo =
    !isMobile && isVideo(bgUrl) && !videoEnded && !videoError;

  // --- 스크롤 리빌 (네 원래 코드 유지용) ---
  const [, setImageVisible] = useState([false, false, false]);
  const imageRefs = useRef<(HTMLDivElement | null)[]>([]);

  useEffect(() => {
    const imageObserver = new window.IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          const idxAttr = entry.target.getAttribute("data-image-idx");
          if (idxAttr == null) return;
          const idx = Number(idxAttr);
          setImageVisible((prev) => {
            if (prev[idx] === entry.isIntersecting) return prev;
            const next = [...prev];
            next[idx] = entry.isIntersecting;
            return next;
          });
        });
      },
      { threshold: 0.3, rootMargin: "0px 0px -10% 0px" }
    );

    imageRefs.current.forEach((el) => el && imageObserver.observe(el));
    return () => imageObserver.disconnect();
  }, []);

  return (
    <section className="relative w-full bg-[#FFFFF0]">
      {/* ✅ PC 전용 비디오 (한 번만 재생) */}
      {showVideo && (
        <div
          className={`absolute inset-0 w-full h-screen transition-opacity duration-700 ${
            videoEnded ? "opacity-0" : "opacity-100"
          }`}
          style={{ zIndex: 1 }}
        >
          <video
            ref={videoRef}
            src={bgUrl}
            autoPlay
            loop={false}
            muted
            playsInline
            preload="auto"
            poster={fallbackImage}
            onEnded={handleVideoEnd}
            onError={handleVideoError}
            className="w-full h-full object-cover"
          />
        </div>
      )}

      {/* ✅ 모바일 + 비디오 끝 + 에러 + 비디오 아닌 경우 → 항상 이미지 */}
    <div
        className={`absolute inset-0 w-full h-screen transition-opacity duration-700 ${
          !showVideo || videoEnded || videoError ? "opacity-100" : "opacity-0"
        }`}
        style={{
          backgroundImage: isMobile ? `url('${mobileMainImage}')` : `url('${fallbackImage}')`,
          backgroundPosition: isMobile ? "center top" : "center",
          backgroundRepeat: "no-repeat",
          backgroundSize: isMobile ? "cover" : "cover", // 🔥 핵심!
          zIndex: 0,
        }}
      />

      {/* 높이 확보 */}
      <div className="h-screen" />
    </section>
  );
}
