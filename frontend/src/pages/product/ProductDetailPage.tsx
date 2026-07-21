import { useEffect, useMemo, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/useAuth";
import Lightbox from "../../components/Lightbox";
import { isMobileDevice } from "../../utils/isMobile";
import { API_BASE_URL } from "../../config/apiBase";

type Cap = {
  id: number;
  name: string;
  price: number;
  pastPrice?: number;
  color: string;
  description: string;
  mainImageUrl: string;
  imageUrls: string[];
  category: string;
  stock: number;
  size: string[];
  sizeInfo: string;
};

const SERVER = API_BASE_URL;

export default function CapDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [cap, setCap] = useState<Cap | null>(null);
  const [expandedImage, setExpandedImage] = useState<string | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [size, setSize] = useState<string>("");
  const [message, setMessage] = useState<string | null>(null);
  const [sizeStocks, setSizeStocks] = useState<{ [size: string]: number }>({});
  const [currentSizeStock, setCurrentSizeStock] = useState<number>(0);
  const [isMobile, setIsMobile] = useState(false);

  // 모바일 여부 감지
  useEffect(() => {
    setIsMobile(isMobileDevice());
  }, []);

  // 상품 + 재고 + 리뷰 로딩
  useEffect(() => {
    if (!id) return;

    // 상품 정보 + 재고
    fetch(`${SERVER}/api/cap/${id}`)
      .then((res) => res.json())
      .then((data) => {
        setCap(data);
        return fetch(`${SERVER}/api/cap/stocks/${id}`);
      })
      .then((res) => res.json())
      .then((stocks) => {
        if (stocks && typeof stocks === "object") {
          setSizeStocks(stocks);
        }
      })
      .catch(() => setCap(null));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  // 사이즈 선택 시 재고 업데이트
  useEffect(() => {
    if (size && sizeStocks[size] !== undefined) {
      const stock = sizeStocks[size];
      setCurrentSizeStock(stock);
      if (quantity > stock) {
        setQuantity(stock > 0 ? 1 : 1);
      }
    } else {
      setCurrentSizeStock(0);
      setQuantity(1);
    }
  }, [size, sizeStocks, quantity]);

  const images = useMemo(
    () =>
      cap
        ? [cap.mainImageUrl, ...(cap.imageUrls ?? [])].filter(Boolean)
        : [],
    [cap]
  );

  const toast = (msg: string) => {
    setMessage(msg);
    setTimeout(() => setMessage(null), 3000);
  };

  // sizeInfo JSON 파싱 함수
  const parseSizeInfo = (sizeInfoStr: string) => {
    try {
      const parsed = JSON.parse(sizeInfoStr);
      return parsed;
    } catch {
      return null;
    }
  };

  const handleAddToCart = async () => {
    if (!user) {
      toast("로그인이 필요합니다!");
      navigate("/login");
      return;
    }
    if (!cap) return;
    if (!size) return toast("사이즈를 선택해 주세요.");

    try {
      const accessToken = localStorage.getItem("access_token");

      // 1. 최신 재고 조회
      const stockRes = await fetch(`${SERVER}/api/cap/stocks/${cap.id}`);
      let availableStock = currentSizeStock;

      if (stockRes.ok) {
        const stocks = await stockRes.json();
        if (
          stocks &&
          typeof stocks === "object" &&
          stocks[size] !== undefined
        ) {
          availableStock = Number(stocks[size]);
        }
      }

      if (availableStock === 0) {
        return toast(`${size} 사이즈가 품절되었습니다.`);
      }

      // 2. 장바구니 현재 수량 조회
      const findUrl = `${SERVER}/api/cart/find?userId=${user.id}&productId=${cap.id}&size=${size}`;
      const findRes = await fetch(findUrl, {
        headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
      });

      let currentQuantity = 0;
      if (findRes.ok) {
        const data = await findRes.json();
        if (typeof data === "number") {
          currentQuantity = data;
        } else if (data && typeof data === "object" && "quantity" in data) {
          currentQuantity = data.quantity ?? 0;
        }
      }

      const totalQuantity = currentQuantity + quantity;
      if (totalQuantity > availableStock) {
        return toast(
          `재고를 초과할 수 없습니다.\n` +
            `장바구니: ${currentQuantity}개 + 추가: ${quantity}개 = ${totalQuantity}개\n` +
            `${size} 사이즈 재고: ${availableStock}개`
        );
      }

      const saveRes = await fetch(`${SERVER}/api/cart/save`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
        },
        body: JSON.stringify({
          userId: user.id,
          productId: cap.id,
          quantity,
          size,
        }),
      });

      saveRes.ok
        ? toast("장바구니에 추가되었습니다!")
        : toast("장바구니 추가에 실패했습니다.");
    } catch (e) {
      console.error(e);
      toast("오류가 발생했습니다.");
    }
  };

  if (!cap) return <div className="py-12 text-center">로딩 중...</div>;

  // ---------- 공통 서브 컴포넌트들 ----------

  const ProductInfo = (
    <>
      <div className="flex items-center gap-2">
        <h1 className="text-lg md:text-xl font-extrabold text-black">
          {cap.name}
        </h1>
        <span className="text-lg md:text-xl font-extrabold text-black">
          - {cap.color}
        </span>
      </div>

      <div className="text-lg md:text-xl font-extrabold mt-4 text-black">
        {cap.pastPrice && cap.pastPrice > cap.price ? (
          <>
            <span className="text-gray-500 line-through mr-3 text-base">{cap.pastPrice.toLocaleString()}₩</span>
            <span className="text-black">{cap.price.toLocaleString()}₩</span>
          </>
        ) : (
          <>
            {cap.price.toLocaleString()}{" "}
            <span className="font-bold text-black">₩</span>
          </>
        )}
      </div>

      {/* SIZE */}
      <div className="mt-4">
        <label className="block text-sm font-bold tracking-wide mb-1 text-white">
          SIZE
        </label>
        <select
          value={size}
          onChange={(e) => setSize(e.target.value)}
          className="w-full px-2 py-1 border-b border-black bg-white text-sm text-black focus:outline-none"
        >
          <option value="">[필수] 옵션선택</option>
          {cap.size && cap.size.length > 0 ? (
  cap.size.map((s: string) => {
    const stock = sizeStocks[s] || 0;
    const isOutOfStock = stock === 0;
    const isLowStock = stock > 0 && stock <= 10;

    return (
      <option
        key={s}
        value={s}
        disabled={isOutOfStock}
        style={isOutOfStock ? { color: "#999" } : {}}
      >
        {s}
        {isOutOfStock ? " (품절)" : isLowStock ? " (품절임박)" : ""}
      </option>
    );
  })
) : (
  <option value="FREE">FREE (One Size)</option>
)}
        </select>
      </div>

      {/* 수량 */}
      <div className="mt-4">
        <label className="block text-sm font-medium mb-1 text-white">수량</label>
        <select
          value={quantity}
          onChange={(e) => setQuantity(Number(e.target.value))}
          className="w-20 px-2 py-1 border-b border-black bg-white text-sm text-black focus:outline-none"
          disabled={!size || currentSizeStock === 0}
        >
          {!size || currentSizeStock === 0 ? (
            <option value={1}>-</option>
          ) : (
            Array.from(
              { length: Math.min(currentSizeStock, 10) },
              (_, i) => i + 1
            ).map((num) => (
              <option key={num} value={num}>
                {num}
              </option>
            ))
          )}
        </select>
      </div>

      {/* 접는 섹션들 */}
      <div className="mt-6 space-y-3">
        <details className="group">
          <summary className="cursor-pointer select-none py-2 font-bold flex items-center justify-between text-white">
            Size <span className="transition-transform group-open:rotate-180">▾</span>
          </summary>
          <div className="pb-3 text-sm text-white/80">
            {(() => {
              const sizeData = parseSizeInfo(cap.sizeInfo);
              if (Array.isArray(sizeData) && sizeData.length > 0) {
                const headers = Object.keys(sizeData[0]);
                return (
                  <table className="w-full border-collapse">
                    <thead>
                      <tr className="border-b border-black/20">
                        {headers.map((header) => (
                          <th
                            key={header}
                            className="text-left py-2 px-2 font-bold"
                          >
                            {header}
                          </th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {sizeData.map((row: any, idx: number) => (
                        <tr key={idx} className="border-b border-black/10">
                          {headers.map((header) => (
                            <td key={header} className="py-2 px-2">
                              {String(row[header] || "")}
                            </td>
                          ))}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                );
              } else if (sizeData && typeof sizeData === "object") {
                return (
                  <table className="w-full border-collapse">
                    <thead>
                      <tr className="border-b border-black/20">
                        <th className="text-left py-2 px-2 font-bold">사이즈</th>
                        <th className="text-left py-2 px-2 font-bold">상세</th>
                      </tr>
                    </thead>
                    <tbody>
                      {Object.entries(sizeData).map(([key, value]) => (
                        <tr key={key} className="border-b border-black/10">
                          <td className="py-2 px-2 font-semibold">{key}</td>
                          <td className="py-2 px-2">{String(value)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                );
              } else {
                return (
                  cap.sizeInfo ||
                  "FREE (머리둘레 조절 가능). * 상세 표는 출시 시 업데이트."
                );
              }
            })()}
          </div>
        </details>

        <details className="group">
          <summary className="cursor-pointer select-none py-2 font-bold flex items-center justify-between text-white">
            Shipping{" "}
            <span className="transition-transform group-open:rotate-180">
              ▾
            </span>
          </summary>
          <div className="pb-3 text-sm text-white/80 whitespace-pre-line">
            {`배송 기간 : 1~3일
배송 비용 : 3,500원 (상품 금액 70,000원 이상 구매 시 무료배송)
배송 지역 : 전국(일부 지역 제외)
산간지역 등 별도의 추가 배송비용을 지불하셔야 하는 경우도 있습니다.

주문 하신 상품은 입금 확인 후, 검수를 거쳐 배송이 준비됩니다. (주말/공휴일 제외한 영업일에 출고가 진행됩니다.)

-송장이 발행된 이후에는 주문 취소가 불가합니다.
-재고가 부족한 경우 취소 처리가 될 수 있습니다.
-주문 후에는 색상 및 사이즈 변경이 어렵습니다.
-교환 및 환불은 공지사항을 참고 바랍니다.`}
          </div>
        </details>

        <details className="group">
          <summary className="cursor-pointer select-none py-2 font-bold flex items-center justify-between text-white">
            Exchange{" "}
            <span className="transition-transform group-open:rotate-180">
              ▾
            </span>
          </summary>
          <div className="pb-3 text-sm text-white/80 whitespace-pre-line">
            {`교환 / 환불 안내
-상품 수령일부터 7일 이내에 교환 환불이 가능합니다.

교환 / 환불 불가 사유
-모자 제품은 착용 후 화장품이나 자국이 생긴경우
-고객님에 의해 제품, 택, 라벨등이 훼손된 경우 (세탁 포함)
-제품 공정에서 발생한 부분 (바느질부분, 워싱 등)은 제품 불량으로 간주되지 않습니다.`}
          </div>
        </details>
      </div>
    </>
  );

  const Buttons = (
    <div className="mt-4 flex gap-6">
      <button
        onClick={handleAddToCart}
        className="flex-1 py-2 bg-black text-white tracking-wide hover:opacity-90 transition-opacity"
        disabled={!size || currentSizeStock === 0}
      >
        {!size
          ? "Select your size"
          : currentSizeStock === 0
          ? "SOLD OUT"
          : "Add To Cart"}
      </button>

      <button
        className="flex-1 py-1 bg-white border border-black tracking-wide hover:bg-gray-100 transition-colors"
        onClick={() => {
          if (!cap || !size || currentSizeStock === 0) return;
          if (!user) {
            navigate(`/login?redirect=/cap/${cap.id}`);
            return;
          }
          navigate("/buy", {
            state: {
              mode: "buy-now",
              item: {
                productId: cap.id,
                productName: cap.name,
                price: cap.price,
                quantity,
                size,
                color: cap.color,
                mainImageUrl: cap.mainImageUrl,
              },
            },
          });
        }}
        disabled={!size || currentSizeStock === 0}
      >
        Buy
      </button>
    </div>
  );

  const ImagesSection = (
    <section className="md:pr-4 mt-6">
      {images.map((src, i) => (
        <figure key={i} className="mb-10 last:mb-0">
          <img
            src={src}
            alt={`${cap.name} ${i + 1}`}
            className="block mx-auto w-full max-w-[900px] h-auto object-contain"
            loading={i === 0 ? "eager" : "lazy"}
          />
        </figure>
      ))}
    </section>
  );

  // ---------- 실제 렌더 ----------

  return (
    <div className="min-h-screen relative bg-transparent font-sans" style={{ fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial" }}>
      {/* ✅ 배경 이미지: 모바일/데스크탑 분리 */}
      <div
        className="fixed inset-0 w-full h-full bg-cover bg-center z-0 md:hidden"
        style={{
          backgroundImage: `url('${SERVER}/images/productdetailbgM.webp')`,
        }}
      />
      <div
        className="fixed inset-0 w-full h-full bg-cover bg-center z-0 hidden md:block"
        style={{
          backgroundImage: `url('${SERVER}/images/productdetailbg.webp')`,
        }}
      />

      {/* 토스트 메시지 */}
      {message && (
        <div className="fixed top-4 right-4 z-50 bg-black text-white px-6 py-3 shadow-lg">
          {message}
        </div>
      )}

      {isMobile ? (
        // 📱 모바일 레이아웃
        <div className="pt-24 pb-12 px-4 relative">
          {/* 메인 이미지들 */}
          <section className="mt-4">
            {images.map((src, i) => (
              <figure key={i} className="mb-6 last:mb-2">
                <img
                  src={src}
                  alt={`${cap.name} ${i + 1}`}
                  className="w-full h-auto object-contain"
                  loading={i === 0 ? "eager" : "lazy"}
                />
              </figure>
            ))}
          </section>

          {/* 상품 정보 + 버튼 */}
          <section className="mt-6">
            {ProductInfo}
            {Buttons}
          </section>

        </div>
      ) : (
        // 💻 데스크탑 레이아웃
        <div
          className="
            relative
            max-w-[1400px] mx-auto
            grid grid-cols-1
            md:grid-cols-[minmax(0,1fr)_minmax(0,900px)_420px]
            gap-0 md:gap-10
            px-4 md:px-6
            pt-24 md:pt-32
            pb-12
          "
        >
          {/* 좌측 여백 */}
          <div className="hidden md:block" aria-hidden />

          {/* 가운데: 이미지 + 리뷰 */}
          {ImagesSection}

          {/* 오른쪽: 상품 정보 */}
          <aside
            className="
              fixed top-4.5 right-10
              md:h-[calc(100vh-15rem)]
              p-0
              bg-transparent
              shadow-none border-none
              md:justify-self-end
              md:translate-x-6
              mt-32
              w-[420px] max-w-[420px]
              flex flex-col
            "
          >
            <div className="flex-none">{ProductInfo}</div>
            {Buttons}
          </aside>
        </div>
      )}

      {/* 라이트박스 */}
      <Lightbox
        imageSrc={expandedImage || ""}
        isOpen={!!expandedImage}
        onClose={() => setExpandedImage(null)}
      />
    </div>
  );
}
