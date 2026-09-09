import { useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { isMobileDevice } from "../../utils/isMobile";
import { API_BASE_URL } from "../../config/apiBase";

const SERVER = API_BASE_URL;

export default function CapPage() {
  const [caps, setCaps] = useState<
    Array<{
      id: number;
      name: string;
      price: number;
      pastPrice?: number;
      mainImageUrl: string;
      color: string;
      stock?: number;
    }>
  >([]);
  const navigate = useNavigate();
  const [isMobile, setIsMobile] = useState(false);

  useEffect(() => {
    fetch(`${SERVER}/api/cap/findAll`)
      .then((res) => res.json())
      .then((data) => {
        const sorted = [...data].sort((a, b) => {
          const soldOutA = (a.stock ?? 0) === 0;
          const soldOutB = (b.stock ?? 0) === 0;
          if (soldOutA !== soldOutB) return soldOutA ? 1 : -1;
          return b.id - a.id;
        });
        setCaps(sorted);
      });
  }, []);

  useEffect(() => {
    setIsMobile(isMobileDevice());
  }, []);

  const displayBg = `${SERVER}/images/emptyload.webp`;

  return (
    <div
      className="min-h-screen relative font-sans"
      style={{
        fontFamily:
          "system-ui, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial",
      }}
    >
      {/* 고정 배경 */}
      <div
        className="fixed inset-0 w-full h-full bg-cover bg-center pointer-events-none"
        style={{
          backgroundImage: `url('${displayBg}')`,
          zIndex: 0,
        }}
      />

      {/* 컨텐츠 */}
      <main
        className={`relative max-w-2xl mx-auto px-4 py-8 ${
          isMobile ? "pt-68" : "pt-24"
        } md:pt-32`}
        style={{ zIndex: 1 }}
      >
        <div className="flex flex-col gap-36 items-center">
          {caps.map((cap) => (
            <div key={cap.id} className="flex flex-col items-center mb-16">
              <img
                src={cap.mainImageUrl}
                alt={cap.name}
                className={
                  isMobile
                    ? "w-full h-auto object-contain mb-4 cursor-pointer"
                    : "w-[700px] h-[500px] object-cover mb-4 cursor-pointer"
                }
                onClick={() => navigate(`/product/${cap.id}`)}
                loading="lazy"
              />

              {/* ✅ 텍스트: 모바일 16px / PC 24px 유지 */}
              <div
                className="
                  text-base md:text-2xl
                  text-center
                  font-bold
                  mt-2 mb-2
                  text-white
                  leading-snug
                  break-keep
                "
              >
                <span>{cap.name}</span>
                <span> - {cap.color}</span>
                {(cap.stock ?? 0) === 0 && (
                  <span className="inline-flex items-center justify-center bg-red-500 rounded-full text-white text-xs font-bold ml-2 h-5 px-1.5">품절</span>
                )}
              </div>
            </div>
          ))}
        </div>
      </main>
    </div>
  );
}
