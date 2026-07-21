import { useEffect, useRef, useState } from "react";
import api from "../../lib/axios";
import { useAuth } from "../../auth/useAuth";
import { API_BASE_URL } from "../../config/apiBase";

type LogbookItem = {
  id: number;
  imageUrl: string;
  sortOrder?: number;
};

const API_BASE = API_BASE_URL;
const SERVER = API_BASE_URL; // 이미지/배경도 같은 서버 사용

type StyleState = {
  scale: number;
  opacity: number;
  translateY: number;
  rotate: number;
  blur: number;
};

// 기준 크기 (원본 크기)
const BASE_WIDTH = 500;
const BASE_HEIGHT = 500;

// 같은 시퀀스를 몇 번 반복해서 보여줄지
const LOOP_COUNT = 4;

export default function LogBookPage() {
  const { user } = useAuth();
  const isAdmin = !!user?.isAdmin;

  const [items, setItems] = useState<LogbookItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 모바일 여부
  const [isMobile, setIsMobile] = useState(false);

  // 각 아이템 DOM 참조 (반복된 아이템 포함 index 기준)
  const itemRefs = useRef<(HTMLDivElement | null)[]>([]);
  // virtualIndex별 스타일 상태
  const [itemStyles, setItemStyles] = useState<Record<number, StyleState>>({});

  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const displayBg = `${SERVER}/images/background.webp`;
  const mobileImage = `${SERVER}/images/mobileLoad.webp`;

  // 모바일 여부 체크
  useEffect(() => {
    const handleResize = () => {
      if (typeof window !== "undefined") {
        setIsMobile(window.innerWidth < 768);
      }
    };
    handleResize();
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  // 로그북 목록 조회
  const fetchLogbookItems = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get(`${API_BASE}/api/logbook`);
      const data = res.data;

      if (Array.isArray(data)) {
        setItems(data as LogbookItem[]);
      } else {
        setItems([]);
        setError(
          "로그북 데이터를 불러오지 못했습니다(응답 형식 오류). Nginx에서 /logbook 가 백엔드로 프록시되는지 확인하세요."
        );
      }
    } catch (e: any) {
      console.error(e);
      setError(e?.message || "로그북 이미지를 불러오는 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // 초기 로딩
  useEffect(() => {
    fetchLogbookItems();
  }, []);

  // 옵션 A: 스냅 + 패럴랙스 + 살짝 회전 + 약한 blur
  // - requestAnimationFrame 무한루프 제거
  // - scroll/resize 이벤트에서 rAF 1회로 계산
  useEffect(() => {
    if (items.length === 0) return;

    let ticking = false;

    const calc = () => {
      const viewportH = window.innerHeight;
      const focusY = viewportH * 0.55; // 화면 55% 지점에 포커스
      const maxDist = viewportH * 0.7;

      const totalCount = items.length * LOOP_COUNT;
      const next: Record<number, StyleState> = {};

      for (let virtualIndex = 0; virtualIndex < totalCount; virtualIndex++) {
        const el = itemRefs.current[virtualIndex];
        if (!el) continue;

        const rect = el.getBoundingClientRect();
        const centerY = rect.top + rect.height / 2;

        const dist = centerY - focusY; // +면 아래, -면 위
        const ad = Math.min(Math.abs(dist) / maxDist, 1); // 0~1
        const ease = ad * ad;

        // 1) scale: 포커스 근처는 살짝 커지는 스냅(최대 +12%)
        const snap = 1 + Math.max(0, 0.12 * (1 - ad));
        let scale = snap - ease * 0.55;
        scale = Math.max(0.55, Math.min(scale, 1.15));

        // 2) opacity: 멀수록 서서히 fade
        let opacity = 1 - ease * 0.9;
        opacity = Math.max(0, Math.min(opacity, 1));

        // 3) parallax: 위로 지나간 애들은 더 빨리 떠오르는 느낌
        const translateY = dist < 0 ? -ease * 80 : 0;

        // 4) rotate: 위로 지나갈수록 아주 살짝 기울어짐
        const rotate = dist < 0 ? -ease * 3 : 0;

        // 5) blur: 위로 지나갈 때만 아주 약하게
        const blur = dist < 0 ? ease * 2 : 0;

        next[virtualIndex] = { scale, opacity, translateY, rotate, blur };
      }

      setItemStyles(next);
      ticking = false;
    };

    const onScrollOrResize = () => {
      if (ticking) return;
      ticking = true;
      requestAnimationFrame(calc);
    };

    window.addEventListener("scroll", onScrollOrResize, { passive: true });
    window.addEventListener("resize", onScrollOrResize);

    // 최초 1회
    calc();

    return () => {
      window.removeEventListener("scroll", onScrollOrResize);
      window.removeEventListener("resize", onScrollOrResize);
    };
  }, [items]);

  // 관리자: 사진 추가 버튼 클릭
  const handleAddClick = () => {
    if (!isAdmin) return;
    fileInputRef.current?.click();
  };

  // 파일 업로드 + logbook 엔티티 생성
  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    setUploading(true);
    setError(null);
    try {
      for (const file of Array.from(files)) {
        const formData = new FormData();
        formData.append("file", file);

        // 1) 실제 파일 업로드
        const upRes = await api.post(`${API_BASE}/api/upload`, formData, {
          headers: { "Content-Type": "multipart/form-data" },
        });

        const imageUrl = upRes.data?.url as string | undefined;
        if (!imageUrl) {
          console.error("업로드 실패: url 없음");
          continue;
        }

        // 2) logbook 엔티티 생성
        await api.post(`${API_BASE}/api/logbook`, {
          imageUrl,
        });
      }

      // 업로드 후 목록 갱신
      await fetchLogbookItems();
    } catch (err: any) {
      console.error(err);
      setError(err?.response?.data || err?.message || "이미지 업로드 중 오류가 발생했습니다.");
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  // 관리자: 이미지 삭제 (DB + 파일)
  const handleDelete = async (item: LogbookItem) => {
    if (!window.confirm("이 로그북 이미지를 삭제하시겠습니까?")) return;

    setError(null);
    try {
      // 1) DB 엔트리 삭제
      await api.delete(`${API_BASE}/api/logbook/${item.id}`);

      // 2) 실제 파일 삭제
      try {
        const urlObj = new URL(item.imageUrl);
        const parts = urlObj.pathname.split("/");
        const filename = parts[parts.length - 1];
        await api.post(`${API_BASE}/api/image/delete`, [filename]);
      } catch (e) {
        console.warn("이미지 파일 삭제 실패(무시 가능):", e);
      }

      // 3) 프론트 목록에서 제거
      setItems((prev) => prev.filter((it) => it.id !== item.id));
    } catch (err: any) {
      console.error(err);
      setError(err?.response?.data || err?.message || "삭제 중 오류가 발생했습니다.");
    }
  };

  // 렌더용 배열: 같은 시퀀스를 여러 번 반복
  const repeatedItems: { item: LogbookItem; virtualIndex: number }[] = [];
  if (items.length > 0) {
    for (let loop = 0; loop < LOOP_COUNT; loop++) {
      items.forEach((item, i) => {
        const virtualIndex = loop * items.length + i;
        repeatedItems.push({ item, virtualIndex });
      });
    }
  }

  return (
    <div className="min-h-screen relative">
      {/* 배경: 도로/풍경 고정 */}
      <div
        className="fixed inset-0 w-full h-full bg-cover bg-center pointer-events-none"
        style={{
          backgroundImage: isMobile ? `url('${mobileImage}')` : `url('${displayBg}')`,
          zIndex: 0,
        }}
      />

      {/* 콘텐츠 영역 */}
      <main
        className={
          isMobile
            ? "relative mx-auto px-2 py-16"
            : "relative max-w-4xl mx-auto px-4 py-24 md:py-32"
        }
        style={{ zIndex: 1 }}
      >
        {/* 상단 관리자 영역 */}
        <div className="flex items-center justify-between mb-8">
          {isAdmin && (
            <div>
              <button
                onClick={handleAddClick}
                disabled={uploading}
                className="px-4 py-2 bg-blue-600 text-white rounded shadow hover:bg-blue-700 disabled:bg-gray-400 font-beaver"
                style={{ fontSize: "18px" }}
              >
                {uploading ? "업로드 중..." : "사진 추가"}
              </button>
              <input
                type="file"
                accept="image/*"
                multiple
                ref={fileInputRef}
                onChange={handleFileChange}
                className="hidden"
              />
            </div>
          )}
        </div>

        {error && (
          <div className="mb-4 text-red-400 bg-black/50 px-3 py-2 rounded text-sm">
            {String(error)}
          </div>
        )}

        {loading ? (
          <p className="text-center text-white">로딩 중...</p>
        ) : items.length === 0 ? (
          <div className="text-center text-white">
            <p>등록된 로그북 이미지가 없습니다.</p>
            {isAdmin && <p>위쪽 &quot;사진 추가&quot; 버튼으로 이미지를 등록해보세요.</p>}
          </div>
        ) : (
          <div
            className={`flex flex-col items-center ${
              isMobile ? "gap-20 mt-16" : "gap-40 mt-40"
            }`}
          >
            {repeatedItems.map(({ item, virtualIndex }) => {
              const style = itemStyles[virtualIndex] ?? {
                scale: 1,
                opacity: 1,
                translateY: 0,
                rotate: 0,
                blur: 0,
              };

              // 박스 크기는 고정, transform으로만 움직임(자연스러움/성능)
              const baseW = isMobile ? BASE_WIDTH * 0.7 : BASE_WIDTH;
              const baseH = isMobile ? BASE_HEIGHT * 0.7 : BASE_HEIGHT;

              return (
                <div
                  key={`${item.id}-${virtualIndex}`}
                  ref={(el) => {
                    itemRefs.current[virtualIndex] = el;
                  }}
                  className="flex flex-col items-center will-change-transform"
                  style={{
                    opacity: style.opacity,
                    transform: `translateY(${style.translateY}px) scale(${style.scale}) rotate(${style.rotate}deg)`,
                    filter: `blur(${style.blur}px)`,
                    transition:
                      "transform 220ms ease-out, opacity 220ms ease-out, filter 220ms ease-out",
                  }}
                >
                  <div
                    className="max-w-full bg-transparent relative"
                    style={{
                      width: `${baseW}px`,
                      height: `${baseH}px`,
                    }}
                  >
                    {/* 관리자 삭제 버튼 */}
                    {isAdmin && (
                      <button
                        onClick={() => handleDelete(item)}
                        className="absolute top-3 right-3 z-10 bg-red-600/80 text-white w-9 h-9 rounded-full flex items-center justify-center hover:bg-red-700 shadow"
                        title="이미지 삭제"
                      >
                        ✕
                      </button>
                    )}

                    <img
                      src={item.imageUrl}
                      alt={`logbook-${item.id}`}
                      className="w-full h-full object-cover"
                      draggable={false}
                      loading="lazy"
                    />
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </main>
    </div>
  );
}
