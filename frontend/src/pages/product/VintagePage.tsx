import { useNavigate, useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { isMobileDevice } from "../../utils/isMobile";
import { API_BASE_URL } from "../../config/apiBase";

const SERVER = API_BASE_URL;
type VintageCategory = "CAP" | "SHIRT";

export default function VintagePage() {
  const { category } = useParams<{ category?: string }>();
  const [vintages, setVintages] = useState<
    Array<{
      id: number;
      name: string;
      price: number;
      pastPrice?: number;
      mainImageUrl: string;
      color: string;
      stock?: number;
      vintageCategory?: VintageCategory;
    }>
  >([]);
  const navigate = useNavigate();
  const [isMobile, setIsMobile] = useState(false);

  useEffect(() => {
    fetch(`${SERVER}/api/vintage/findAll`)
      .then((res) => res.json())
      .then((data) => {
        const sorted = [...data].sort((a, b) => {
          const soldOutA = (a.stock ?? 0) === 0;
          const soldOutB = (b.stock ?? 0) === 0;
          if (soldOutA !== soldOutB) return soldOutA ? 1 : -1;
          return b.id - a.id;
        });
        setVintages(sorted);
      });
  }, []);

  useEffect(() => {
    setIsMobile(isMobileDevice());
  }, []);

  const displayBg = `${SERVER}/images/emptyload.webp`;
  const selectedCategory =
    category?.toUpperCase() === "CAP" || category?.toUpperCase() === "SHIRT"
      ? (category.toUpperCase() as VintageCategory)
      : "ALL";
  const visibleVintages = vintages.filter((vintage) => {
    if (selectedCategory === "ALL") return true;
    return vintage.vintageCategory === selectedCategory;
  });

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
        className={`relative max-w-7xl mx-auto px-4 py-8 ${
          isMobile ? "pt-68" : "pt-24"
        } md:pt-32`}
        style={{ zIndex: 1 }}
      >
        <div className="grid grid-cols-2 xl:grid-cols-3 gap-x-3 gap-y-8 md:gap-x-8 md:gap-y-14 items-start">
          {visibleVintages.map((vintage) => (
            <div key={vintage.id} className="flex flex-col items-center">
              <img
                src={vintage.mainImageUrl}
                alt={vintage.name}
                className="w-full aspect-[4/5] object-cover mb-4 cursor-pointer"
                onClick={() => navigate(`/product/${vintage.id}`)}
                loading="lazy"
              />

              <div
                className="
                  text-sm md:text-xl
                  text-center
                  font-bold
                  mt-2 mb-2
                  text-white
                  leading-snug
                  break-keep
                "
              >
                <span>{vintage.name}</span>
                {(vintage.stock ?? 0) === 0 && (
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
