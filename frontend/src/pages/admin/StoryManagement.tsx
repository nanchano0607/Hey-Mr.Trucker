import { useEffect, useMemo, useState } from "react";
import { API_BASE_URL } from "../../config/apiBase";
import api from "../../lib/axios";

const SERVER = API_BASE_URL;

type StoryContentResponse = {
  url?: string;
};

type StoryBackgroundResponse = {
  url?: string;
};

function isVideoUrl(url: string) {
  const clean = (url || "").split(/[?#]/)[0].toLowerCase();
  return clean.endsWith(".mp4") || clean.endsWith(".webm") || clean.endsWith(".ogg");
}

function filenameFromUrl(url: string) {
  const clean = (url || "").split(/[?#]/)[0];
  const idx = clean.lastIndexOf("/");
  return idx >= 0 ? clean.substring(idx + 1) : clean;
}

async function uploadToCap(file: File): Promise<string> {
  const fd = new FormData();
  fd.append("file", file);

  try {
    const res = await api.post(`/api/upload`, fd, {
      // Content-Type은 자동으로 FormData boundary로 설정되도록 비워둡니다.
      validateStatus: (s) => s >= 200 && s < 300,
    });

    const data = res.data as { url?: string; filename?: string };
    if (data?.url) return data.url;

    // 일부 서버는 filename만 반환할 수 있음. 이 경우 public URL로 보정.
    if (data?.filename) {
      const name = (data.filename || "").trim();
      if (name) return `${SERVER}/uploads/cap/${encodeURIComponent(name)}`; // 서버 설정에 맞춰 필요시 조정
    }

    throw new Error("upload_no_url");
  } catch (e: any) {
    const status = e?.response?.status;
    const msg = e?.response?.data?.message || e?.message || "unknown";
    throw new Error(`upload_failed: ${status ?? ""} ${msg}`);
  }
}

async function deleteImages(namesOrUrls: string[]): Promise<{ success: string[]; fail: string[] }>
{
  try {
    const res = await api.post(
      `/api/image/delete`,
      namesOrUrls,
      { validateStatus: (s) => s >= 200 && s < 300 }
    );
    const data = res.data as { success?: string[]; fail?: string[] };
    return {
      success: data.success ?? [],
      fail: data.fail ?? [],
    };
  } catch (e) {
    console.error("deleteImages error", e);
    return { success: [], fail: namesOrUrls };
  }
}

interface StoryManagementProps {
  isOpen: boolean;
  onToggle: () => void;
}

export default function StoryManagement({ isOpen, onToggle }: StoryManagementProps) {
  const [bgUrl, setBgUrl] = useState<string>("");
  const [contentUrl, setContentUrl] = useState<string>("");

  const [bgFile, setBgFile] = useState<File | null>(null);
  const [contentFile, setContentFile] = useState<File | null>(null);

  const [loading, setLoading] = useState<boolean>(false);

  const contentIsVideo = useMemo(() => isVideoUrl(contentUrl), [contentUrl]);

  const fetchCurrent = async () => {
    try {
          const res = await api.get(`/api/story`, { validateStatus: (s) => s >= 200 && s < 300 });
      const data = res.data as { backgroundImage?: string; contentImage?: string };

      const toUrl = (name?: string) =>
        name && name.trim()
          ? `${SERVER}/uploads/cap/${encodeURIComponent(name.trim())}`
          : "";

      setBgUrl(toUrl(data?.backgroundImage));
      setContentUrl(toUrl(data?.contentImage));
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    if (isOpen) fetchCurrent();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen]);

  const applyBackground = async () => {
    if (!bgFile) {
      alert("배경 파일을 선택해주세요.");
      return;
    }

    setLoading(true);
    try {
      // 1) 기존 배경 파일 삭제 시도 (URL 또는 이름 전달 가능)
      if (bgUrl) {
        const del = await deleteImages([bgUrl]);
        if (del.fail.length) {
          console.warn("배경 삭제 실패:", del.fail);
        }
      }

      // 2) 새 파일 업로드
      const uploadedUrl = await uploadToCap(bgFile);
      const filename = filenameFromUrl(uploadedUrl);

      // 3) 스토리 엔티티에 파일명 저장
      const res = await api.post(
        `/api/story/background`,
        null,
        { params: { filename }, validateStatus: (s) => s >= 200 && s < 300 }
      );

      const data = res.data as StoryBackgroundResponse;
      setBgUrl(data?.url ?? uploadedUrl);
      alert("스토리 배경이 적용되었습니다.");
      setBgFile(null);
    } catch (e) {
      console.error(e);
      alert(
        `스토리 배경 업로드/적용 중 오류가 발생했습니다.\n${
          (e as any)?.message || ""
        }`
      );
    } finally {
      setLoading(false);
    }
  };

  const applyContent = async () => {
    if (!contentFile) {
      alert("콘텐츠 파일(이미지/동영상)을 선택해주세요.");
      return;
    }

    setLoading(true);
    try {
      // 1) 기존 콘텐츠 파일 삭제 시도
      if (contentUrl) {
        const del = await deleteImages([contentUrl]);
        if (del.fail.length) {
          console.warn("콘텐츠 삭제 실패:", del.fail);
        }
      }

      // 2) 새 파일 업로드
      const uploadedUrl = await uploadToCap(contentFile);
      const filename = filenameFromUrl(uploadedUrl);

      // 3) 스토리 엔티티에 파일명 저장
      const res = await api.post(
        `/api/story/content`,
        null,
        { params: { filename }, validateStatus: (s) => s >= 200 && s < 300 }
      );

      const data = res.data as StoryContentResponse;
      setContentUrl(data?.url ?? uploadedUrl);
      alert("스토리 콘텐츠가 적용되었습니다.");
      setContentFile(null);
    } catch (e) {
      console.error(e);
      alert(
        `스토리 콘텐츠 업로드/적용 중 오류가 발생했습니다.\n${
          (e as any)?.message || ""
        }`
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="border p-4 rounded font-sans">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <h2 className="text-xl font-semibold">스토리 관리</h2>
        </div>
        <button
          onClick={onToggle}
          className="px-3 py-1 border rounded text-sm bg-blue-300 hover:bg-blue-200"
        >
          {isOpen ? "접기" : "펼치기"}
        </button>
      </div>

      {isOpen && (
        <div className="mt-4 space-y-6">
          <div className="border rounded p-3">
            <div className="flex items-center justify-between gap-2">
              <h3 className="font-semibold">배경</h3>
              <button
                onClick={fetchCurrent}
                disabled={loading}
                className="px-3 py-1 border rounded text-sm bg-blue-300 hover:bg-blue-200 disabled:opacity-60"
              >
                새로고침
              </button>
            </div>

            <div className="mt-3">
              <div className="text-sm text-gray-700 mb-2">현재 배경:</div>
              <div className="w-full h-40 bg-gray-200 rounded overflow-hidden">
                {bgUrl ? (
                  <div
                    className="w-full h-full bg-cover bg-center"
                    style={{ backgroundImage: `url('${bgUrl}')` }}
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-sm text-gray-600">
                    설정된 배경이 없습니다.
                  </div>
                )}
              </div>
            </div>

            <div className="mt-3 flex items-center gap-2">
              <input
                type="file"
                accept="image/*"
                onChange={(e) => setBgFile(e.target.files?.[0] ?? null)}
                className="text-sm"
              />
              <button
                onClick={applyBackground}
                disabled={loading}
                className="px-3 py-1 border rounded text-sm bg-blue-300 hover:bg-blue-200 disabled:opacity-60"
              >
                적용
              </button>
            </div>
          </div>

          <div className="border rounded p-3">
            <div className="flex items-center justify-between gap-2">
              <h3 className="font-semibold">테두리 안 콘텐츠</h3>
            </div>

            <div className="mt-3">
              <div className="text-sm text-gray-700 mb-2">현재 콘텐츠:</div>
              <div className="w-full h-56 bg-gray-200 rounded overflow-hidden">
                {contentUrl ? (
                  contentIsVideo ? (
                    <video
                      src={contentUrl}
                      className="w-full h-full object-cover"
                      autoPlay
                      loop
                      muted
                      playsInline
                      controls
                    />
                  ) : (
                    <img
                      src={contentUrl}
                      alt="story content"
                      className="w-full h-full object-cover"
                    />
                  )
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-sm text-gray-600">
                    설정된 콘텐츠가 없습니다.
                  </div>
                )}
              </div>
            </div>

            <div className="mt-3 flex items-center gap-2">
              <input
                type="file"
                accept="image/*,video/*"
                onChange={(e) => setContentFile(e.target.files?.[0] ?? null)}
                className="text-sm"
              />
              <button
                onClick={applyContent}
                disabled={loading}
                className="px-3 py-1 border rounded text-sm bg-blue-300 hover:bg-blue-200 disabled:opacity-60"
              >
                적용
              </button>
            </div>

            <div className="mt-2 text-xs text-gray-600">
              업로드는 `/api/upload`(cap 폴더)로 진행됩니다.
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
