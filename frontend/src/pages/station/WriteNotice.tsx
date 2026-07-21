import { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../lib/axios";
import { useAuth } from "../../auth/useAuth";
import { API_BASE_URL } from "../../config/apiBase";

const SERVER = API_BASE_URL;

export default function WriteNotice() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [busy, setBusy] = useState(false);

  const handleSubmit = async () => {
    if (!user?.isAdmin) return alert("관리자만 작성할 수 있습니다.");
    if (!title.trim() || !content.trim()) return alert("제목과 내용을 입력하세요.");
    try {
      setBusy(true);
      const body = { title: title.trim(), content: content.trim() };
      await api.post(`${SERVER}/api/notices`, body, { params: { userId: user.id } });
      alert("작성되었습니다.");
      navigate("/notices");
    } catch (err: any) {
      console.error(err);
      alert(err?.response?.data?.error || "작성 실패");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div
      className="min-h-screen bg-cover bg-center bg-no-repeat flex items-end"
      style={{ backgroundImage: `url(${SERVER}/images/emptyload.webp)` }}
    >
      <div className="w-full">
        <div className="mx-auto bg-white shadow-2xl" style={{ width: "60%", borderRadius: 6 }}>
          <div className="p-6">
            <h2 className="text-xl font-semibold mb-4">공지 작성</h2>
            {!user?.isAdmin ? (
              <div className="text-center text-red-600">관리자만 접근할 수 있습니다.</div>
            ) : (
              <div>
                <input
                  className="w-full border px-2 py-1 mb-2"
                  placeholder="제목"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  disabled={busy}
                />
                <textarea
                  className="w-full border px-2 py-1 mb-2 h-48"
                  placeholder="내용"
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  disabled={busy}
                />
                <div className="flex items-center justify-end space-x-2">
                  <button className="px-3 py-1 bg-gray-200 rounded" onClick={() => navigate(-1)} disabled={busy}>
                    취소
                  </button>
                  <button className="px-4 py-2 bg-blue-600 text-white rounded" onClick={handleSubmit} disabled={busy}>
                    작성
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
