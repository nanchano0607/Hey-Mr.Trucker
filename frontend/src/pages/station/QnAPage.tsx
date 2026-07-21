import { useState, useEffect } from "react";
import api from "../../lib/axios";
import { useAuth } from "../../auth/useAuth";
import { useNavigate } from "react-router-dom";
import { API_BASE_URL } from "../../config/apiBase";

const SERVER = API_BASE_URL;

type Notice = {
  id: number;
  title: string;
  content: string;
  author?: string;
  createdAt: string;
  updatedAt?: string;
};

export default function QnAPage() {
  const { user } = useAuth();
  const [notices, setNotices] = useState<Notice[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 5;

  // no inline create form any more; admins use separate write page

  const [fetchError, setFetchError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    fetchNotices();
  }, []);

  // Use native fetch for listing to avoid axios interceptor side-effects during diagnosis
  const fetchNotices = async () => {
    try {
      setLoading(true);
      setFetchError(null);
      const res = await fetch(`${SERVER}/api/notices`, { credentials: "include" });
      if (!res.ok) {
        const text = await res.text().catch(() => "");
        throw new Error(`status=${res.status} body=${text}`);
      }

      const ct = res.headers.get("content-type") || "";
      if (!ct.includes("application/json")) {
        const text = await res.text().catch(() => "");
        throw new Error(`non-json response content-type=${ct} body=${text}`);
      }

      const data: unknown = await res.json();
      if (!Array.isArray(data)) {
        throw new Error("응답이 배열이 아닙니다. (Nginx 라우팅/백엔드 엔드포인트 확인 필요)");
      }
      // latest first
      const list = data as Notice[];
      console.log("Fetched notices:", list); // 데이터 확인용
      list.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
      setNotices(list);
      setCurrentPage(1);
    } catch (err: any) {
      console.error("Failed to load notices:", err);
      setFetchError(err?.message || String(err));
    } finally {
      setLoading(false);
    }
  };

  const totalPages = Math.max(1, Math.ceil(notices.length / itemsPerPage));
  const startIdx = (currentPage - 1) * itemsPerPage;
  const paged = notices.slice(startIdx, startIdx + itemsPerPage);

  // creation handled on separate WriteNotice page

  const handleDelete = async (id: number) => {
    if (!user?.isAdmin) return alert("관리자만 삭제할 수 있습니다.");
    if (!confirm("정말 삭제하시겠습니까?")) return;

    try {
      await api.delete(`${SERVER}/api/notices/${id}`, { params: { userId: user.id } });
      await fetchNotices();
    } catch (err: any) {
      console.error(err);
      alert(err?.response?.data?.error || "삭제 실패");
    }
  };

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
              <div className="text-center">Loading notices...</div>
            ) : fetchError ? (
              <div className="text-center text-red-600">
                로드 실패: {fetchError}
                <div className="mt-2">
                  <button className="px-3 py-1 bg-gray-200 rounded" onClick={fetchNotices}>
                    다시 시도
                  </button>
                </div>
              </div>
            ) : notices.length === 0 ? (
              <div className="text-center">등록된 공지사항이 없습니다.</div>
            ) : (
              <>
                <div className="flex-1 overflow-hidden">
                  <div className="flex flex-col h-full justify-start">
                    {paged.map((n) => (
                      <div key={n.id} className="border-b">
                        <div className="flex items-center justify-between py-3">
                          <div
                            className="font-semibold text-2xl text-gray-800 cursor-pointer"
                            onClick={() => navigate(`/qna/${n.id}`)}
                          >
                            {n.title}
                          </div>
                          <div className="flex items-center space-x-4">
                            <div className="text-sm text-gray-500">{new Date(n.createdAt).toLocaleDateString()}</div>
                            {user?.isAdmin && (
                              <button
                                className="px-2 py-1 bg-red-500 text-white rounded"
                                onClick={() => handleDelete(n.id)}
                              >
                                삭제
                              </button>
                            )}
                          </div>
                        </div>
                        <div className="text-xs text-gray-400 pb-2">작성자: {n.author || "알 수 없음"}</div>
                      </div>
                    ))}
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
                        className={
                          (page === currentPage
                            ? "px-3 py-1 bg-gray-800 text-white rounded"
                            : "px-3 py-1 bg-gray-200 rounded") + " focus:outline-none"
                        }
                      >
                        {page}
                      </button>
                    ))}
                  </div>
                </div>
              </>
            )}

            {/* Admin-only write button (navigates to separate write page) */}
            {user?.isAdmin && (
              <div className="mt-4 border-t pt-4 flex justify-end">
                <button
                  className="px-4 py-2 bg-blue-600 text-white rounded"
                  onClick={() => navigate(`/qna/write`)}
                >
                  공지 작성
                </button>
              </div>
            )}
                </div>
              </div>
            </div>
          </div>

      {/* Mobile (md 미만) : 간단한 스택 리스트 */}
      <div
        className="block md:hidden h-screen bg-cover bg-center font-sans"
        style={{ backgroundImage: `url(${SERVER}/images/emptyload.webp)` }}
      >
        <div className="h-full overflow-y-auto p-4 pt-20">
          <div className="max-w-xl mx-auto space-y-4">
            <h2 className="text-2xl font-bold text-white">Q&A</h2>

            {loading ? (
              <div className="text-white/80">로딩 중...</div>
            ) : fetchError ? (
              <div className="text-red-400">로드 실패: {fetchError}</div>
            ) : notices.length === 0 ? (
              <div className="text-white">등록된 공지사항이 없습니다.</div>
            ) : (
              <div className="space-y-3">
                {paged.map((n) => (
                  <div key={n.id} className="p-4 bg-black/50 rounded-lg">
                    <div className="flex items-start justify-between">
                      <div className="flex-1 cursor-pointer" onClick={() => navigate(`/qna/${n.id}`)}>
                        <div className="font-semibold text-white text-lg">{n.title}</div>
                        <div className="text-xs text-white/70 mt-1">{new Date(n.createdAt).toLocaleDateString()}</div>
                      </div>
                      {user?.isAdmin && (
                        <button className="ml-3 px-3 py-1 rounded bg-red-500 text-white" onClick={() => handleDelete(n.id)}>
                          삭제
                        </button>
                      )}
                    </div>
                  </div>
                ))}

                {/* Mobile pagination */}
                <div className="flex items-center justify-center space-x-2 mt-2">
                  {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                    <button
                      key={page}
                      onClick={() => setCurrentPage(page)}
                      className={
                        (page === currentPage ? "px-3 py-1 bg-white/90 text-black rounded" : "px-3 py-1 bg-white/20 text-white rounded") + " focus:outline-none"
                      }
                    >
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
