import { useState, useEffect } from "react";
import { API_BASE_URL } from "../../config/apiBase";

const SERVER = API_BASE_URL;

type Review = {
  id: number;
  userId: number;
  userName?: string;
  productId: number;
  productName?: string;
  orderId?: number;
  selectedSize?: string;
  rating: number;
  content: string;
  imageUrls: string[];
  createdAt: string; // LocalDateTime ISO string from backend
  updatedAt?: string;
};

export default function ReviewPage() {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [loading, setLoading] = useState(true);
  const [fetchError, setFetchError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 5;

  const resolveImageUrl = (raw: string) => {
    if (!raw) return "";
    if (raw.startsWith("http")) return raw;
    if (raw.startsWith("/")) return `${SERVER}${raw}`;
    return `${SERVER}/${raw}`;
  };

  useEffect(() => {
    const fetchReviews = async () => {
      try {
        setFetchError(null);
        // Backend returns ReviewResponse DTOs at /reviews
        const response = await fetch(`${SERVER}/api/reviews`, { credentials: "include" });
        if (!response.ok) {
          const text = await response.text().catch(() => "");
          throw new Error(`status=${response.status} body=${text}`);
        }

        const ct = response.headers.get("content-type") || "";
        if (!ct.includes("application/json")) {
          const text = await response.text().catch(() => "");
          throw new Error(`non-json response content-type=${ct} body=${text}`);
        }

        const data: unknown = await response.json();
        if (!Array.isArray(data)) {
          throw new Error("응답이 배열이 아닙니다. (Nginx 라우팅/백엔드 엔드포인트 확인 필요)");
        }
        // Sort latest first
        const list = data as Review[];
        list.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        setReviews(list);
        setCurrentPage(1);
      } catch (error) {
        console.error("Error fetching reviews:", error);
        setFetchError(error instanceof Error ? error.message : String(error));
        setReviews([]);
      } finally {
        setLoading(false);
      }
    };

    fetchReviews();
  }, []);

  const totalPages = Math.max(1, Math.ceil(reviews.length / itemsPerPage));
  useEffect(() => {
    if (currentPage > totalPages) setCurrentPage(totalPages);
  }, [totalPages, currentPage]);

  const startIdx = (currentPage - 1) * itemsPerPage;
  const pagedReviews = reviews.slice(startIdx, startIdx + itemsPerPage);

  return (
    <>
      {/* Desktop (md 이상) */}
      <div
        className="hidden md:flex md:flex-col md:justify-end min-h-screen bg-cover bg-center bg-no-repeat font-sans pt-32 pb-8"
        style={{ backgroundImage: `url(${SERVER}/images/emptyload.webp)` }}
      >
        <div className="w-full">
          <div className="mx-auto bg-gray-300 shadow-2xl" style={{ width: "50%", height: "600px" }}>
            <div className="p-6 flex flex-col h-full">
              {loading ? (
                <div className="text-center">Loading reviews...</div>
              ) : fetchError ? (
                <div className="text-center text-red-600">
                  로드 실패: {fetchError}
                  <div className="mt-2 text-sm text-slate-500">
                    `/reviews`가 index.html로 떨어지면 이런 오류가 납니다. (Nginx 프록시 경로 확인)
                  </div>
                </div>
              ) : reviews.length === 0 ? (
                <div className="text-center">No reviews yet.</div>
              ) : (
                <>
                  <div className="flex-1 overflow-auto">
                    <div className="flex flex-col">
                      {pagedReviews.map((review) => (
                        <div key={review.id} className="flex items-start py-4 border-b">
                          <div className="w-24 h-24 mr-4 flex-shrink-0 bg-gray-100 overflow-hidden flex items-center justify-center">
                            {review.imageUrls && review.imageUrls.length > 0 ? (
                              <img
                                src={resolveImageUrl(review.imageUrls[0])}
                                alt={`Review ${review.id}`}
                                className="w-full h-full object-cover"
                              />
                            ) : (
                              <div className="text-gray-400">사진 없음</div>
                            )}
                          </div>
                          <div className="flex-1">
                            <div className="flex items-center justify-between">
                              <div className="text-yellow-500">
                                {"★".repeat(review.rating)}
                                {"☆".repeat(5 - review.rating)}
                              </div>
                              <div className="text-sm text-gray-500">{new Date(review.createdAt).toLocaleDateString()}</div>
                            </div>
                            <p className="text-gray-800 mt-2">{review.content}</p>
                            <div className="text-sm text-gray-400 mt-2">
                              사용자: {review.userName ?? review.userId} · 상품: {review.productName ?? review.productId}
                              {review.selectedSize ? ` · 사이즈: ${review.selectedSize}` : ""}
                            </div>
                          </div>
                        </div>
                      ))}
                      {/* if less than itemsPerPage, empty space remains below — reviews stay at top */}
                    </div>
                  </div>

                  {/* Pagination controls */}
                  <div className="mt-4">
                    <div className="flex items-center justify-center space-x-2">
                      {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                        <button
                          key={page}
                          onClick={() => setCurrentPage(page)}
                          aria-current={page === currentPage ? "page" : undefined}
                          className={(page === currentPage ? "px-3 py-1 bg-gray-800 text-white rounded" : "px-3 py-1 bg-gray-200 rounded") + " focus:outline-none"}
                        >
                          {page}
                        </button>
                      ))}
                    </div>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Mobile (md 미만) */}
      <div className="block md:hidden h-screen bg-cover bg-center font-sans" style={{ backgroundImage: `url(${SERVER}/images/emptyload.webp)` }}>
        <div className="h-full overflow-y-auto p-4 pt-20">
          <div className="max-w-xl mx-auto space-y-3">
            <h2 className="text-2xl font-bold text-white">Reviews</h2>

            {loading ? (
              <div className="text-white/80">로딩 중...</div>
            ) : fetchError ? (
              <div className="text-red-400">로드 실패: {fetchError}</div>
            ) : reviews.length === 0 ? (
              <div className="text-white">등록된 리뷰가 없습니다.</div>
            ) : (
              <div className="space-y-3">
                {pagedReviews.map((review) => (
                  <div key={review.id} className="p-3 bg-black/50 rounded-lg flex items-start gap-3">
                    <div className="w-16 h-16 bg-gray-100 flex-shrink-0 overflow-hidden">
                      {review.imageUrls && review.imageUrls.length > 0 ? (
                        <img src={resolveImageUrl(review.imageUrls[0])} alt={`Review ${review.id}`} className="w-full h-full object-cover" />
                      ) : (
                        <div className="text-gray-400 p-2">사진 없음</div>
                      )}
                    </div>
                    <div className="flex-1">
                      <div className="flex items-center justify-between">
                        <div className="text-yellow-400">{"★".repeat(review.rating)}{"☆".repeat(5 - review.rating)}</div>
                        <div className="text-xs text-white/80">{new Date(review.createdAt).toLocaleDateString()}</div>
                      </div>
                      <div className="text-white mt-2">{review.content}</div>
                      <div className="text-xs text-white/60 mt-2">
                        사용자: {review.userName ?? review.userId} · 상품: {review.productName ?? review.productId}
                        {review.selectedSize ? ` · 사이즈: ${review.selectedSize}` : ""}
                      </div>
                    </div>
                  </div>
                ))}

                <div className="flex items-center justify-center space-x-2 mt-2">
                  {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                    <button key={page} onClick={() => setCurrentPage(page)} className={(page === currentPage ? 'px-3 py-1 bg-white/90 text-black rounded' : 'px-3 py-1 bg-white/20 text-white rounded') + ' focus:outline-none'}>
                      {page}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </>
  );
}
