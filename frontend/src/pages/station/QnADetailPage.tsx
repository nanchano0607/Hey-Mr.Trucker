import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import api from "../../lib/axios";
import { useAuth } from "../../auth/useAuth";
import { API_BASE_URL } from "../../config/apiBase";

const SERVER = API_BASE_URL;

type Notice = {
  id: number;
  title: string;
  content: string;
  userId?: number;
  createdAt: string;
};

export default function QnADetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [notice, setNotice] = useState<Notice | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    fetchDetail();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const fetchDetail = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await fetch(`${SERVER}/api/notices/${id}`, { credentials: "include" });
      if (!res.ok) {
        const text = await res.text().catch(() => "");
        throw new Error(`status=${res.status} body=${text}`);
      }
      const data: Notice = await res.json();
      setNotice(data);
    } catch (err: any) {
      console.error("Failed to load notice detail:", err);
      setError(err?.message || String(err));
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!notice) return;
    if (!user?.isAdmin) return alert("관리자만 삭제할 수 있습니다.");
    if (!confirm("정말 삭제하시겠습니까?")) return;
    try {
      await api.delete(`${SERVER}/api/notices/${notice.id}`, { params: { userId: user.id } });
      alert("삭제되었습니다.");
      navigate(-1);
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
            <div className="p-6 overflow-auto h-full">
              {loading ? (
                <div className="text-center">Loading...</div>
              ) : error ? (
                <div className="text-center text-red-600">로드 실패: {error}</div>
              ) : !notice ? (
                <div className="text-center">공지사항을 찾을 수 없습니다.</div>
              ) : (
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center space-x-3">
                      <button
                        onClick={() => navigate(-1)}
                        aria-label="뒤로가기"
                        className="p-1 rounded hover:bg-gray-100"
                      >
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-gray-700" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                          <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
                        </svg>
                      </button>
                      <h1 className="text-2xl font-bold">{notice.title}</h1>
                    </div>
                    <div className="text-sm text-gray-500">{new Date(notice.createdAt).toLocaleDateString()}</div>
                  </div>
                  <div className="prose max-w-none whitespace-pre-wrap text-gray-700">{notice.content}</div>
                  <div className="text-sm text-gray-400 mt-4">작성자 ID: {notice.userId}</div>
                  {user?.isAdmin && (
                    <div className="mt-4 flex justify-end">
                      <button className="px-3 py-1 bg-red-500 text-white rounded" onClick={handleDelete}>
                        삭제
                      </button>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Mobile (md 미만) */}
      <div
        className="block md:hidden h-screen bg-cover bg-center font-sans"

        style={{ backgroundImage: `url(${SERVER}/images/emptyload.webp)` }}
      >
        <div className="h-full overflow-y-auto p-4 pt-20">
          <div className="max-w-xl mx-auto bg-white shadow rounded p-4">
            {loading ? (
              <div className="text-center">Loading...</div>
            ) : error ? (
              <div className="text-center text-red-600">로드 실패: {error}</div>
            ) : !notice ? (
              <div className="text-center">공지사항을 찾을 수 없습니다.</div>
            ) : (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <button onClick={() => navigate(-1)} className="p-1 rounded bg-gray-100">
                      ←
                    </button>
                    <h1 className="text-lg font-bold">{notice.title}</h1>
                  </div>
                  <div className="text-sm text-gray-500">{new Date(notice.createdAt).toLocaleDateString()}</div>
                </div>
                <div className="whitespace-pre-wrap text-gray-700">{notice.content}</div>
                <div className="text-sm text-gray-400">작성자 ID: {notice.userId}</div>
                {user?.isAdmin && (
                  <div className="flex justify-end">
                    <button className="px-3 py-1 bg-red-500 text-white rounded" onClick={handleDelete}>삭제</button>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </>
  );
}
